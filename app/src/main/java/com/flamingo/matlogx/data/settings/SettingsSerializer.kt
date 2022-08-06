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

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore

import com.flamingo.matlogx.data.log.LogBuffer
import com.flamingo.matlogx.data.log.LogLevel
import com.google.protobuf.InvalidProtocolBufferException

import java.io.InputStream
import java.io.OutputStream

const val DEFAULT_LOG_SIZE = 10000
val DEFAULT_LOG_LEVEL = LogLevel.VERBOSE
const val DEFAULT_INCLUDE_DEVICE_INFO = false
val DEFAULT_BUFFERS = listOf(
    LogBuffer.MAIN,
    LogBuffer.SYSTEM,
    LogBuffer.CRASH
)
const val DEFAULT_EXPANDED = false
const val DEFAULT_TEXT_SIZE = 12
const val DEFAULT_WRITE_BUFFER_SIZE = 200

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.newBuilder()
        .addAllLogBuffers(DEFAULT_BUFFERS)
        .setLogSizeLimit(DEFAULT_LOG_SIZE)
        .setLogLevel(DEFAULT_LOG_LEVEL)
        .setIncludeDeviceInfo(DEFAULT_INCLUDE_DEVICE_INFO)
        .setExpandedByDefault(DEFAULT_EXPANDED)
        .setTextSize(DEFAULT_TEXT_SIZE)
        .setWriteBufferSize(DEFAULT_WRITE_BUFFER_SIZE)
        .build()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot parse settings proto", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings",
    serializer = SettingsSerializer
)