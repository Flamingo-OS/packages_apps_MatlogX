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
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope

import com.flamingo.matlogx.R
import com.flamingo.matlogx.data.log.LogcatRepository
import com.flamingo.matlogx.ui.LogcatActivity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.koin.android.ext.android.inject

class LogRecordService : LifecycleService() {

    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var activityIntent: PendingIntent
    private lateinit var stopIntent: PendingIntent

    private val logcatRepository by inject<LogcatRepository>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_RECORDING) {
                stopRecording()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
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
        registerReceiver(broadcastReceiver, IntentFilter(ACTION_STOP_RECORDING))
    }

    private fun setupNotificationChannel() {
        notificationManager = NotificationManagerCompat.from(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action != ACTION_RECORD_LOGS) {
            START_NOT_STICKY
        } else {
            if (!logcatRepository.recordingLogs.value) {
                startForeground()
                receiveFailureEvents()
                lifecycleScope.launch(Dispatchers.IO) {
                    logcatRepository.recordLogs()
                }
            }
            super.onStartCommand(intent, flags, startId)
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(activityIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_matlogx)
            .setContentTitle(getString(R.string.recording_logs))
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_baseline_stop_24,
                getString(R.string.stop),
                stopIntent
            )
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun receiveFailureEvents() {
        lifecycleScope.launch {
            for (throwable in logcatRepository.recordLogErrorChannel) {
                Toast.makeText(
                    this@LogRecordService,
                    throwable.localizedMessage ?: getString(R.string.failed_to_save_log),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun stopRecording() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancelAll()
        stopSelf()
    }

    override fun onDestroy() {
        logcatRepository.stopRecordingLogs()
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    companion object {
        const val ACTION_RECORD_LOGS = "com.flamingo.matlogx.action.RECORD_LOGS"
        private const val ACTION_STOP_RECORDING = "com.flamingo.matlogx.action.STOP_RECORDING"

        private val CHANNEL_ID = LogRecordService::class.qualifiedName!!
        private const val CHANNEL_NAME = "MatlogX log recorder"
        private const val NOTIFICATION_ID = 1

        private const val ACTIVITY_REQUEST_CODE = 1
        private const val STOP_REQUEST_CODE = 2
    }
}