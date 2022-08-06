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

package com.flamingo.matlogx.data.settings

import android.content.Context
import com.flamingo.matlogx.data.log.LogBuffer
import com.flamingo.matlogx.data.log.LogLevel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(context: Context) {

    private val settingsDataStore = context.settingsDataStore

    val logcatBuffers: Flow<List<LogBuffer>> = settingsDataStore.data.map { it.logBuffersList }

    val logcatSizeLimit: Flow<Int> = settingsDataStore.data.map { it.logSizeLimit }

    val logLevel: Flow<LogLevel> = settingsDataStore.data.map { it.logLevel }

    val expandedByDefault: Flow<Boolean> = settingsDataStore.data.map { it.expandedByDefault }

    val includeDeviceInfo: Flow<Boolean> = settingsDataStore.data.map { it.includeDeviceInfo }

    val textSize: Flow<Int> = settingsDataStore.data.map { it.textSize }

    val writeBufferSize: Flow<Int> = settingsDataStore.data.map { it.writeBufferSize }

    suspend fun setLogcatBuffers(buffers: List<LogBuffer>) {
        settingsDataStore.updateData {
            it.toBuilder()
                .clearLogBuffers()
                .addAllLogBuffers(buffers)
                .build()
        }
    }

    /**
     * Set the logcat size limit.
     *
     * @param limit the new limit to save.
     */
    suspend fun setLogcatSizeLimit(limit: Int) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setLogSizeLimit(limit)
                .build()
        }
    }

    suspend fun setLogLevel(level: LogLevel) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setLogLevel(level)
                .build()
        }
    }

    /**
     * Save include device information preference.
     *
     * @param include the value to save.
     */
    suspend fun setIncludeDeviceInfo(include: Boolean) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setIncludeDeviceInfo(include)
                .build()
        }
    }

    /**
     * Save whether to expand log message by default.
     *
     * @param expanded the value to save.
     */
    suspend fun setExpandedByDefault(expanded: Boolean) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setExpandedByDefault(expanded)
                .build()
        }
    }

    /**
     * Save text size.
     *
     * @param textSize the value to save.
     */
    suspend fun setTextSize(textSize: Int) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setTextSize(textSize)
                .build()
        }
    }

    /**
     * Save write buffer size.
     *
     * @param size the value to save.
     */
    suspend fun setWriteBufferSize(size: Int) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setWriteBufferSize(size)
                .build()
        }
    }
}