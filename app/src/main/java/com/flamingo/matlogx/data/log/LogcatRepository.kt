/*
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flamingo.matlogx.data.log

import android.content.Context
import android.net.Uri

import com.flamingo.matlogx.data.settings.settingsDataStore

import java.io.File

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LogcatRepository(
    context: Context,
    private val logFileManager: LogFileManager
) {

    private val settingsDataStore = context.settingsDataStore
    private val contentResolver = context.contentResolver

    private val _recordingLogs = MutableStateFlow(false)
    val recordingLogs: StateFlow<Boolean> = _recordingLogs.asStateFlow()

    val recordLogErrorChannel = Channel<Throwable>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Get an asynchronous stream of [Log].
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param logLevel the logLevel of log below which logs should be omitted.
     * @return a flow of [Log].
     */
    fun getLogcatStream(streamConfig: StreamConfig): Flow<Log> {
        return LogcatReader.readAsFlow(
            streamConfig.args(),
            streamConfig.tags,
            streamConfig.logLevel,
            streamConfig.filter,
            streamConfig.filterIgnoreCase
        ).flowOn(Dispatchers.IO)
    }

    /**
     * Saves given list of [Log] as a zip file.
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param logLevel the logLevel of log below which logs should be omitted.
     * @param includeDeviceInfo whether to include device info inside the zip.
     * @return a result with the [File] (or an exception if failed) that was saved.
     */
    suspend fun saveLogAsZip(
        tags: List<String>?,
        logLevel: LogLevel,
        includeDeviceInfo: Boolean,
    ): Result<Uri> =
        withContext(Dispatchers.IO) {
            logFileManager.saveZip(
                LogcatReader.getRawLogs(getLogcatArgs(), tags, logLevel),
                includeDeviceInfo,
            )
        }

    suspend fun recordLogs() {
        _recordingLogs.value = true
        val recordingFileUriResult = withContext(Dispatchers.IO) {
            logFileManager.getNewRecordingFileUri()
        }
        recordingFileUriResult.onFailure {
            recordLogErrorChannel.send(it)
            _recordingLogs.value = false
            return
        }
        val outputStreamResult = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(recordingFileUriResult.getOrThrow(), "wa")
            }
        }
        outputStreamResult.onFailure {
            recordLogErrorChannel.send(it)
            _recordingLogs.value = false
            return
        }
        val outputStream = outputStreamResult.getOrNull() ?: run {
            recordLogErrorChannel.send(Throwable("Failed to open output stream"))
            _recordingLogs.value = false
            return
        }
        outputStream.bufferedWriter().use { writer ->
            val writeBuffer = settingsDataStore.data.map { it.writeBufferSize }.first()
            val logLevel = settingsDataStore.data.map { it.logLevel }.first()
            val bufferedLogs = mutableListOf<String>()
            try {
                LogcatReader.readRawLogsAsFlow(
                    getLogcatArgs(),
                    null /* tags aren't supported yet*/,
                    logLevel
                ).collect {
                    bufferedLogs.add(it)
                    if (bufferedLogs.size >= writeBuffer) {
                        bufferedLogs.forEach { log ->
                            writer.write(log)
                            writer.newLine()
                        }
                        writer.flush()
                        bufferedLogs.clear()
                    }
                }
            } catch (e: Throwable) {
                recordLogErrorChannel.send(e)
                _recordingLogs.value = false
                return
            }
        }
    }

    fun stopRecordingLogs() {
        _recordingLogs.value = false
    }

    private suspend fun getLogcatArgs(): Map<String, String?> {
        val buffers = settingsDataStore.data
            .map { it.logBuffersList }
            .first()
            .joinToString(",") { it.name.lowercase() }
        return mapOf(LogcatReader.OPTION_BUFFER to buffers)
    }

    fun getSavedLogsDirectoryUri(): Result<Uri> = logFileManager.getLogDirUri()
}

data class StreamConfig(
    private val logBuffers: List<LogBuffer>,
    val logLevel: LogLevel,
    val tags: List<String>,
    val filter: String? = null,
    val filterIgnoreCase: Boolean = true,
) {
    fun args(): Map<String, String> {
        val buffers = logBuffers.joinToString(",") { it.name.lowercase() }
        return mapOf(LogcatReader.OPTION_BUFFER to buffers)
    }
}