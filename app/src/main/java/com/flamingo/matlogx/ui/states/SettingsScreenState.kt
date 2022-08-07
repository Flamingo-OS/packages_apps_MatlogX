/*
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.flamingo.matlogx.ui.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.flamingo.matlogx.data.log.LogBuffer

import com.flamingo.matlogx.data.settings.SettingsRepository
import com.flamingo.support.compose.ui.preferences.Entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

import org.koin.androidx.compose.get

class SettingsScreenState(
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope
) {

    val logcatBufferEntries = LogBuffer.values()
        .filter { it != LogBuffer.UNRECOGNIZED }
        .map {
            Entry(it.name.lowercase(), it)
        }

    val logcatBuffers: Flow<List<LogBuffer>>
        get() = settingsRepository.logcatBuffers

    val logcatSizeLimit: Flow<Int>
        get() = settingsRepository.logcatSizeLimit

    val expandedByDefault: Flow<Boolean>
        get() = settingsRepository.expandedByDefault

    val textSize: Flow<Int>
        get() = settingsRepository.textSize

    fun setLogcatBuffers(buffers: List<LogBuffer>) {
        coroutineScope.launch {
            settingsRepository.setLogcatBuffers(buffers)
        }
    }

    fun setLogcatSizeLimit(limit: Int) {
        coroutineScope.launch {
            settingsRepository.setLogcatSizeLimit(limit)
        }
    }

    fun setExpandedByDefault(expanded: Boolean) {
        coroutineScope.launch {
            settingsRepository.setExpandedByDefault(expanded)
        }
    }

    fun setTextSize(textSize: Int) {
        coroutineScope.launch {
            settingsRepository.setTextSize(textSize)
        }
    }
}

@Composable
fun rememberSettingsScreenState(
    settingsRepository: SettingsRepository = get(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(settingsRepository, coroutineScope) {
    SettingsScreenState(
        settingsRepository = settingsRepository,
        coroutineScope = coroutineScope
    )
}