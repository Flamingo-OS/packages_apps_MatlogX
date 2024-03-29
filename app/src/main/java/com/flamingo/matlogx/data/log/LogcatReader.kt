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

import java.io.InputStream

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

private const val LOGCAT_BIN = "logcat"

/**
 * Command line options and supported values for logcat binary.
 * There are many other options besides those given here, these
 * are the only one's being used right now.
 * Use of these options can be seen with logcat --help command.
 */
const val OPTION_BUFFER = "-b"

//--Make it public if needed--//
private const val OPTION_DEFAULT_SILENT = "-s"

private const val OPTION_DUMP = "-d"

const val OPTION_RECENT_LINES = "-T"

/**
 * Read logcat with the given command line args and parse
 * each line into a [Log].
 *
 * @param args command line arguments for logcat.
 * @param tags a list of string tags to filter the logs.
 * @param logLevel the log logLevel below which the logs should be discarded.
 * @return a [Flow] of [Log].
 */
fun readAsFlow(
    args: Map<String, String?>,
    tags: List<String>,
    logLevel: LogLevel,
    filter: String?,
    filterIgnoreCase: Boolean
): Flow<Log> {
    return flow {
        getInputStream(args, tags, logLevel)
            .bufferedReader()
            .use {
                while (currentCoroutineContext().isActive) {
                    runCatching {
                        it.readLine()
                    }.onSuccess {
                        if (filter == null || it.contains(
                                filter,
                                ignoreCase = filterIgnoreCase
                            )
                        ) {
                            emit(LogFactory.fromString(it))
                        }
                    }
                }
            }
    }
}

/**
 * Read logcat with the given command line args, and returns
 * the stream as it is.
 *
 * @param args command line arguments for logcat.
 * @param tags a list of string tags to filter the logs.
 * @param logLevel the log logLevel below which the logs should be discarded.
 * @return a [Flow] of [String].
 */
fun readRawLogsAsFlow(
    args: Map<String, String?>,
    tags: List<String>,
    logLevel: LogLevel,
): Flow<String> {
    return flow {
        getInputStream(args, tags, logLevel).bufferedReader().use {
            while (true) {
                runCatching {
                    it.readLine()
                }.getOrNull()?.let { line ->
                    emit(line)
                }
            }
        }
    }
}

/**
 * Get a snapshot of current logcat stream.
 *
 * @param args command line arguments for logcat.
 * @param tags a list of string tags to filter the logs.
 * @param logLevel the log logLevel below which the logs should be discarded.
 * @return current system logs joined to a string.
 */
fun getRawLogs(
    args: Map<String, String?>,
    tags: List<String>,
    logLevel: LogLevel,
): String {
    return getInputStream(args, tags, logLevel, dump = true)
        .bufferedReader()
        .use {
            it.readText()
        }
}

private fun getInputStream(
    args: Map<String, String?>,
    tags: List<String>,
    logLevel: LogLevel,
    dump: Boolean = false,
): InputStream {
    val argsList = mutableListOf(
        LOGCAT_BIN,
        "*:${logLevel.name.first()}",
        "--format=time",
        "-D"
    )
    // Append args
    args.forEach { (k, v) ->
        argsList.add(k)
        argsList.add(v ?: "")
    }
    // Append tags
    if (tags.isNotEmpty()) {
        argsList.add(OPTION_DEFAULT_SILENT)
        tags.forEach { argsList.add(it) }
    }
    // Dump and close stream if specified
    if (dump) {
        argsList.add(OPTION_DUMP)
    }
    val process = ProcessBuilder("/bin/sh", "-c", argsList.joinToString(" ")).start()
    process.outputStream.close()
    return process.inputStream
}