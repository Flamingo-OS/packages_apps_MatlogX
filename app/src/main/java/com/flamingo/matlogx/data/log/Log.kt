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

sealed interface Log {

    val message: String

    fun hasString(string: String?, ignoreCase: Boolean = true): Boolean {
        return string?.let { message.contains(it, ignoreCase = ignoreCase) } ?: true
    }

    data class Divider(override val message: String) : Log

    data class Data(
        val pid: Short?,
        val time: String?,
        val tag: String?,
        val logLevel: LogLevel?,
        override val message: String
    ) : Log
}

object LogFactory {
    private val timeRegex = Regex("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}")
    private val pidRegex = Regex("\\(\\s*\\d+\\)")

    fun fromString(logLine: String): Log {
        // Filter buffer separators
        if (logLine.startsWith("-")) {
            return Log.Divider(message = logLine)
        }
        // Log format:
        // DD-MM HH:MM:SS D/TAG( PID): message
        val metadata = logLine.substringBefore("/")
        val pid = pidRegex.find(logLine)?.value
            ?.substringAfter("(")
            ?.substringBefore(")")
            ?.trim()
            ?.toShortOrNull()
        val logLevel = metadata.lastOrNull()
        return Log.Data(
            pid = pid,
            time = timeRegex.find(metadata)?.value,
            // Assuming that no one insane used ( in their tag
            tag = logLine.substringAfter("/").substringBefore("("),
            logLevel = LogLevel.values().find { it.name.first() == logLevel },
            message = logLine.substringAfter("):").trim(),
        )
    }
}