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

import android.net.Uri

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LogcatRepository(private val logFileManager: LogFileManager) {

    fun getLogcatStream(streamConfig: StreamConfig): Flow<Log> {
        return readAsFlow(
            streamConfig.args(),
            streamConfig.tags,
            streamConfig.logLevel,
            streamConfig.filter,
            streamConfig.filterIgnoreCase
        ).flowOn(Dispatchers.IO)
    }

    fun getRawLogsAsFlow(streamConfig: StreamConfig): Flow<String> {
        return readRawLogsAsFlow(
            streamConfig.args(),
            streamConfig.tags,
            streamConfig.logLevel
        ).flowOn(Dispatchers.IO)
    }

    suspend fun saveLogAsZip(
        streamConfig: StreamConfig,
        includeDeviceInfo: Boolean,
    ): Result<Uri> =
        withContext(Dispatchers.IO) {
            logFileManager.saveZip(
                getRawLogs(
                    streamConfig.args(),
                    streamConfig.tags,
                    streamConfig.logLevel
                ),
                includeDeviceInfo,
            )
        }

    suspend fun getSavedLogsDirectoryUri(): Result<Uri> {
        return withContext(Dispatchers.IO) {
            logFileManager.getLogDirUri()
        }
    }

    suspend fun obtainNewRecordingFileUri(): Result<Uri> {
        return withContext(Dispatchers.IO) {
            logFileManager.obtainNewRecordingFileUri()
        }
    }
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
        return mapOf(OPTION_BUFFER to buffers)
    }
}