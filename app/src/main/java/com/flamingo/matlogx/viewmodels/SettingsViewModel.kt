/*
 * Copyright (C) 2021-2022 FlamingoOS Project
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

package com.flamingo.matlogx.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.flamingo.matlogx.data.settings.SettingsRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val logcatBuffers: Flow<String>
        get() = settingsRepository.getLogcatBuffers()

    val logcatSizeLimit: Flow<Int>
            get() = settingsRepository.getLogcatSizeLimit()

    val expandedByDefault: Flow<Boolean>
        get() = settingsRepository.getExpandedByDefault()

    val textSize: Flow<Int>
        get() = settingsRepository.getTextSize()

    val writeBufferSize: Flow<Int>
        get() = settingsRepository.getWriteBufferSize()

    fun setLogcatBuffers(buffers: String) {
        viewModelScope.launch {
            settingsRepository.setLogcatBuffers(buffers)
        }
    }

    fun setLogcatSizeLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.setLogcatSizeLimit(limit)
        }
    }

    fun setExpandedByDefault(expanded: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExpandedByDefault(expanded)
        }
    }

    fun setTextSize(textSize: Int) {
        viewModelScope.launch {
            settingsRepository.setTextSize(textSize)
        }
    }

    fun setWriteBufferSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setWriteBufferSize(size)
        }
    }
}