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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope

import com.flamingo.matlogx.R
import com.flamingo.matlogx.data.log.LogcatRepository
import com.flamingo.matlogx.data.log.StreamConfig
import com.flamingo.matlogx.data.settings.SettingsRepository
import com.flamingo.matlogx.ui.LogcatActivity

import java.io.BufferedWriter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.koin.android.ext.android.inject

class LogRecordService : LifecycleService() {

    private val logcatRepository by inject<LogcatRepository>()
    private val settingsRepository by inject<SettingsRepository>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_RECORDING) {
                stopRecording()
                stopSelf()
            }
        }
    }

    private val _recording = MutableStateFlow(false)
    val recording = _recording.asStateFlow()

    private lateinit var oldConfig: Configuration
    private lateinit var serviceBinder: ServiceBinder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var activityIntent: PendingIntent
    private lateinit var stopIntent: PendingIntent

    private var recordingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        oldConfig = resources.configuration
        serviceBinder = ServiceBinder()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
        createIntents()
        registerReceiver(broadcastReceiver, IntentFilter(ACTION_STOP_RECORDING))
    }

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.log_record_service_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun createIntents() {
        activityIntent = PendingIntent.getActivity(
            this,
            ACTIVITY_REQUEST_CODE,
            Intent(this, LogcatActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        stopIntent = PendingIntent.getBroadcast(
            this,
            STOP_REQUEST_CODE,
            Intent(ACTION_STOP_RECORDING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.diff(oldConfig) == ActivityInfo.CONFIG_LOCALE) {
            createNotificationChannel()
        }
        oldConfig = newConfig
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return serviceBinder
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(activityIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_matlogx)
            .setContentTitle(getString(R.string.recording_logs))
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                R.drawable.ic_baseline_stop_24,
                getString(R.string.stop),
                stopIntent
            )
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    fun startRecording() {
        if (_recording.value) return
        _recording.value = true
        startForeground()
        if (recordingJob?.isActive != true) {
            recordingJob = lifecycleScope.launch(Dispatchers.IO) {
                startRecordingInternal()
            }
        }
    }

    private suspend fun startRecordingInternal() {
        val recordingFileUriResult = logcatRepository.obtainNewRecordingFileUri().onFailure {
            toastAndStopRecording(it)
            return
        }
        val outputStreamResult = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(recordingFileUriResult.getOrThrow(), "wa")
            }
        }.onFailure {
            toastAndStopRecording(it)
        }
        val outputStream = outputStreamResult.getOrNull() ?: run {
            toastAndStopRecording(Throwable("Failed to open output stream"))
            return
        }
        outputStream.bufferedWriter().use { writer ->
            val writeBufferSize = settingsRepository.writeBufferSize.first()
            val streamConfig = StreamConfig(
                logBuffers = settingsRepository.logcatBuffers.first(),
                logLevel = settingsRepository.logLevel.first(),
                tags = emptyList()
            )
            val bufferedLogs = mutableListOf<String>()
            runCatching {
                logcatRepository.getRawLogsAsFlow(streamConfig).collect {
                    bufferedLogs.add(it)
                    if (bufferedLogs.size >= writeBufferSize) {
                        writeFromBuffer(bufferedLogs.toList(), writer)
                        bufferedLogs.clear()
                    }
                }
            }.onFailure {
                toastAndStopRecording(it)
                return@use
            }
        }
    }

    private suspend fun toastAndStopRecording(throwable: Throwable) {
        withContext(Dispatchers.Main) {
            toast(throwable.localizedMessage ?: getString(R.string.failed_to_save_log))
        }
        stopRecording()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private suspend fun writeFromBuffer(buffer: List<String>, writer: BufferedWriter) {
        withContext(Dispatchers.IO) {
            writer.write(buffer.joinToString("\n"))
        }
    }

    fun stopRecording() {
        if (!_recording.value) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        recordingJob?.cancel()
        recordingJob = null
        _recording.value = false
        toast(getString(R.string.recorded_logs))
    }

    override fun onDestroy() {
        stopRecording()
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    inner class ServiceBinder : Binder() {
        val service: LogRecordService
            get() = this@LogRecordService
    }

    companion object {
        private val CHANNEL_ID = "${LogRecordService::class.qualifiedName!!}_NotificationChannel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP_RECORDING = "com.flamingo.matlogx.action.STOP_RECORDING"
        private const val ACTIVITY_REQUEST_CODE = 1
        private const val STOP_REQUEST_CODE = 2
    }
}