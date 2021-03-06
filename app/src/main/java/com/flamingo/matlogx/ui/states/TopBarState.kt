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

package com.flamingo.matlogx.ui.states

import android.content.Context
import android.content.Intent

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController

import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.flamingo.matlogx.R
import com.flamingo.matlogx.services.LogRecordService
import com.flamingo.matlogx.ui.Routes
import com.flamingo.matlogx.viewmodels.LogcatViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TopBarState(
    private val logcatViewModel: LogcatViewModel,
    private val navHostController: NavHostController,
    private val coroutineScope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
    private val context: Context
) {

    val searchSuggestions: Flow<List<String>>
        get() = logcatViewModel.searchSuggestions

    val logcatStreamPaused: Flow<Boolean>
        get() = logcatViewModel.logcatStreamPaused

    val recordingLogs: Flow<Boolean>
        get() = logcatViewModel.recordingLogs

    fun toggleLogcatFlowState() {
        logcatViewModel.toggleLogcatFlowState()
    }

    fun clearLogs() {
        logcatViewModel.clearLogs()
    }

    fun openSettings() {
        navHostController.navigate(Routes.SETTINGS)
    }

    fun handleSearch(query: String) {
        if (query.isNotBlank()) {
            logcatViewModel.saveRecentSearchQuery(query)
        }
        logcatViewModel.handleSearch(query)
    }

    fun clearSearch() {
        logcatViewModel.handleSearch(null)
    }

    fun clearRecentSearch(query: String) {
        logcatViewModel.clearRecentSearch(query)
    }

    fun clearAllRecentSearches() {
        logcatViewModel.clearAllRecentSearches()
    }

    fun startRecordingLogs() {
        context.startService(
            Intent(context, LogRecordService::class.java).setAction(
                LogRecordService.ACTION_RECORD_LOGS
            )
        )
    }

    fun stopRecordingLogs() {
        context.stopService(Intent(context, LogRecordService::class.java))
    }

    fun openSavedLogs() {
        val uriResult = logcatViewModel.getSavedLogsDirectoryUri()
        if (uriResult.isSuccess) {
            val intent = Intent(Intent.ACTION_VIEW, uriResult.getOrThrow())
            val resolvedActivities =
                context.packageManager.queryIntentActivities(intent, 0 /* flags */)
            if (resolvedActivities.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.activity_not_found))
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    uriResult.exceptionOrNull()?.localizedMessage
                        ?: context.getString(R.string.failed_to_open_directory)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberTopBarState(
    logcatViewModel: LogcatViewModel,
    navHostController: NavHostController = rememberAnimatedNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    context: Context = LocalContext.current
) = remember(logcatViewModel, snackbarHostState, context) {
    TopBarState(logcatViewModel, navHostController, coroutineScope, snackbarHostState, context)
}