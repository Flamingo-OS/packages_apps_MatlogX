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

package com.flamingo.matlogx.ui.screens

import android.content.Intent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.flamingo.matlogx.R
import com.flamingo.matlogx.services.LogRecordService
import com.flamingo.matlogx.ui.states.LogcatScreenState
import com.flamingo.matlogx.ui.widgets.MenuItem
import com.flamingo.matlogx.ui.widgets.OverflowMenu
import com.flamingo.matlogx.ui.widgets.SearchBar
import com.flamingo.support.compose.runtime.rememberBoundService

import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    state: LogcatScreenState,
    onShowLogLevelMenuRequest: () -> Unit,
    onSaveLogsRequest: () -> Unit,
    onShareLogsRequest: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColorForContainer = contentColorFor(backgroundColor = containerColor)
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    SmallTopAppBar(
        title = {
            AnimatedContent(targetState = searchBarExpanded) { expanded ->
                if (expanded) {
                    val recentSearches by state.recentSearchList.collectAsState(emptyList())
                    SearchBar(
                        initialText = "",
                        hint = stringResource(id = R.string.search_hint),
                        history = recentSearches,
                        onSearchRequest = {
                            state.handleSearch(it)
                        },
                        onDismissRequest = {
                            searchBarExpanded = false
                            state.handleSearch(null)
                        },
                        onClearRecentQueryRequest = {
                            state.clearRecentSearch(it)
                        },
                        onClearAllRecentQueriesRequest = {
                            state.clearAllRecentSearches()
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color.Transparent,
                            focusedLeadingIconColor = contentColorForContainer,
                            unfocusedLeadingIconColor = contentColorForContainer,
                            placeholderColor = contentColorForContainer,
                            textColor = contentColorForContainer,
                            cursorColor = contentColorForContainer,
                            focusedTrailingIconColor = contentColorForContainer,
                            unfocusedTrailingIconColor = contentColorForContainer,
                            unfocusedBorderColor = contentColorForContainer,
                            focusedBorderColor = contentColorForContainer
                        )
                    )
                } else {
                    Text(text = stringResource(id = R.string.app_name))
                }
            }
        },
        actions = {
            AnimatedVisibility(visible = !searchBarExpanded, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { searchBarExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_button_content_desc),
                        tint = contentColorForContainer
                    )
                }
            }
            IconButton(onClick = { state.toggleLogcatFlowState() }) {
                val isPaused by state.logcatStreamPaused.collectAsState(false)
                Icon(
                    painter = painterResource(
                        if (isPaused)
                            R.drawable.ic_baseline_play_arrow_24
                        else
                            R.drawable.ic_baseline_pause_24
                    ),
                    contentDescription = stringResource(R.string.play_pause_button_content_desc),
                    tint = contentColorForContainer
                )
            }
            TopBarOverflowMenu(
                state,
                contentColorForContainer,
                onShowLogLevelMenuRequest,
                onSaveLogsRequest,
                onShareLogsRequest
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColorForContainer
        )
    )
}

@Composable
fun TopBarOverflowMenu(
    state: LogcatScreenState,
    menuIconTint: Color,
    onShowLogLevelMenuRequest: () -> Unit,
    onSaveLogsRequest: () -> Unit,
    onShareLogsRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    OverflowMenu(
        overflowIcon = {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.menu_overflow_button),
                tint = menuIconTint
            )
        },
        expanded = menuExpanded,
        onOverflowIconClicked = {
            if (!menuExpanded) menuExpanded = true
        },
        onDismissRequest = {
            menuExpanded = false
        },
    ) {
        MenuItem(
            title = stringResource(id = R.string.clear_logs),
            iconContentDescription = stringResource(id = R.string.clear_logs_button_content_desc),
            imageVector = Icons.Filled.Clear,
            onClick = {
                menuExpanded = false
                state.clearLogs()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.log_level),
            iconContentDescription = stringResource(id = R.string.log_level_button_content_desc),
            imageVector = Icons.Filled.List,
            onClick = {
                menuExpanded = false
                onShowLogLevelMenuRequest()
            }
        )
        val context = LocalContext.current
        val serviceIntent = remember(context) {
            Intent(context, LogRecordService::class.java)
        }
        val service = rememberBoundService(
            context = context,
            intent = serviceIntent,
            obtainService = { (it as LogRecordService.ServiceBinder).service }
        )
        val recordingLogs by (service?.recording ?: emptyFlow()).collectAsState(false)
        MenuItem(
            title = stringResource(id = if (!recordingLogs) R.string.record_logs else R.string.stop_recording),
            iconContentDescription = stringResource(id = R.string.record_logs_button_content_desc),
            painter = painterResource(id = if (!recordingLogs) R.drawable.ic_baseline_circle_24 else R.drawable.ic_baseline_stop_24),
            onClick = {
                if (recordingLogs) {
                    service?.stopRecording()
                } else {
                    context.startService(serviceIntent)
                    service?.startRecording()
                }
                menuExpanded = false
            }
        )
        MenuItem(
            title = stringResource(id = R.string.share_logs),
            iconContentDescription = stringResource(id = R.string.share_button_content_desc),
            imageVector = Icons.Filled.Share,
            onClick = {
                menuExpanded = false
                onShareLogsRequest()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.save_zip),
            iconContentDescription = stringResource(id = R.string.save_zip_button_content_desc),
            painter = painterResource(id = R.drawable.ic_baseline_folder_24),
            onClick = {
                menuExpanded = false
                onSaveLogsRequest()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.saved_logs),
            iconContentDescription = stringResource(id = R.string.saved_logs_button_content_desc),
            painter = painterResource(id = R.drawable.ic_baseline_file_open_24),
            onClick = {
                menuExpanded = false
                state.openSavedLogs()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.settings),
            iconContentDescription = stringResource(id = R.string.clear_logs_button_content_desc),
            imageVector = Icons.Filled.Settings,
            onClick = {
                menuExpanded = false
                state.openSettings()
            }
        )
    }
}