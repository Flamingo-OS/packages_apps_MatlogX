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

package com.flamingo.matlogx.services

import android.service.quicksettings.TileService
import android.widget.Toast

import com.flamingo.matlogx.R
import com.flamingo.matlogx.data.log.LogcatRepository
import com.flamingo.matlogx.data.log.StreamConfig
import com.flamingo.matlogx.data.settings.SettingsRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import org.koin.android.ext.android.inject

class LogTileService : TileService() {

    private val logcatRepository by inject<LogcatRepository>()
    private val settingsRepository by inject<SettingsRepository>()

    private lateinit var serviceScope: CoroutineScope

    private var toast: Toast? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onStartListening() {
        qsTile.apply {
            subtitle = getString(R.string.save_logs)
            updateTile()
        }
    }

    override fun onClick() {
        serviceScope.launch { saveLogs() }
    }

    private suspend fun saveLogs() {
        val includeDeviceInfo = settingsRepository.includeDeviceInfo.first()
        val streamConfig = StreamConfig(
            logBuffers = settingsRepository.logcatBuffers.first(),
            logLevel = settingsRepository.logLevel.first(),
            tags = emptyList()
        )
        val result = logcatRepository.saveLogAsZip(
            streamConfig,
            includeDeviceInfo,
        )
        if (result.isSuccess) {
            toast(getString(R.string.log_saved_successfully))
        } else {
            toast(
                result.exceptionOrNull()?.localizedMessage ?: getString(R.string.failed_to_save_log)
            )
        }
    }

    private fun toast(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also {
            it.show()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}