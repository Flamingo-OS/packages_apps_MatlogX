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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

import com.flamingo.matlogx.R
import com.flamingo.matlogx.data.log.Log
import com.flamingo.matlogx.data.log.LogLevel
import com.flamingo.matlogx.data.settings.DEFAULT_LOG_LEVEL
import com.flamingo.matlogx.ui.states.LogData
import com.flamingo.matlogx.ui.states.LogcatScreenState
import com.flamingo.matlogx.ui.states.rememberLogcatScreenState

import kotlin.math.absoluteValue
import kotlin.math.sign

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogcatScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    logcatScreenState: LogcatScreenState = rememberLogcatScreenState()
) {
    var showLogLevelDialog by rememberSaveable { mutableStateOf(false) }
    if (showLogLevelDialog) {
        val logLevel by logcatScreenState.logLevel.collectAsState(DEFAULT_LOG_LEVEL)
        LogLevelDialog(
            currentLevel = logLevel,
            onDismissRequest = { showLogLevelDialog = false },
            onLogLevelSelected = {
                showLogLevelDialog = false
                logcatScreenState.setLogLevel(it)
            },
            logLevels = logcatScreenState.logLevels
        )
    }
    var showZipSaveDialog by rememberSaveable { mutableStateOf(false) }
    if (showZipSaveDialog) {
        val includeDeviceInfo by logcatScreenState.includeDeviceInfo.collectAsState(false)
        ZipSaveAndShareDialog(
            title = stringResource(id = R.string.save_zip),
            includeDeviceInfo = includeDeviceInfo,
            onIncludeDeviceInfoRequest = {
                logcatScreenState.setIncludeDeviceInfo(it)
            },
            onDismissRequest = {
                showZipSaveDialog = false
            },
            onConfirmRequest = {
                showZipSaveDialog = false
                logcatScreenState.saveLogs()
            }
        )
    }
    var showShareZipDialog by rememberSaveable { mutableStateOf(false) }
    if (showShareZipDialog) {
        val includeDeviceInfo by logcatScreenState.includeDeviceInfo.collectAsState(false)
        ZipSaveAndShareDialog(
            title = stringResource(id = R.string.save_zip),
            includeDeviceInfo = includeDeviceInfo,
            onIncludeDeviceInfoRequest = {
                logcatScreenState.setIncludeDeviceInfo(it)
            },
            onDismissRequest = {
                showShareZipDialog = false
            },
            onConfirmRequest = {
                showShareZipDialog = false
                logcatScreenState.shareLogs()
            }
        )
    }
    if (!logcatScreenState.hasReadLogsPermission) {
        PermissionDialog(onQuitAppRequest = onBackPressed) {
            logcatScreenState.copyCommand()
        }
    }
    val listState = rememberLazyListState()
    val logcatList by logcatScreenState.logsList.collectAsState(emptyList())
    var scrollDirection by remember { mutableStateOf(0f) }
    val fabState by remember {
        derivedStateOf {
            if (listState.isScrollInProgress) {
                when (scrollDirection) {
                    1f -> FABState.Up
                    -1f -> FABState.Down
                    else -> FABState.Gone
                }
            } else {
                FABState.Gone
            }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                logcatScreenState,
                onShowLogLevelMenuRequest = {
                    showLogLevelDialog = true
                },
                onSaveLogsRequest = {
                    showZipSaveDialog = true
                },
                onShareLogsRequest = {
                    showShareZipDialog = true
                }
            )
        },
        floatingActionButton = {
            ScrollFAB(
                fabState = fabState,
                onClick = {
                    if (fabState is FABState.Up) {
                        logcatScreenState.coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    } else if (fabState is FABState.Down) {
                        logcatScreenState.coroutineScope.launch {
                            if (logcatList.isNotEmpty()) {
                                listState.animateScrollToItem(logcatList.lastIndex)
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = logcatScreenState.snackbarHostState)
        }
    ) { paddingValues ->
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    scrollDirection = if (available.y.absoluteValue < 0.1f) {
                        0f
                    } else {
                        available.y.sign
                    }
                    return super.onPreScroll(available, source)
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            state = listState,
            contentPadding = paddingValues
        ) {
            items(logcatList) { item ->
                LogItem(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onExpansionChanged = {
                        item.isExpanded = it
                    },
                )
            }
        }
    }
}

@Composable
fun LogLevelDialog(
    logLevels: Map<String, LogLevel>,
    currentLevel: LogLevel,
    onDismissRequest: () -> Unit,
    onLogLevelSelected: (LogLevel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
        title = {
            Text(text = stringResource(id = R.string.log_level))
        },
        text = {
            Column {
                logLevels.forEach { (name, level) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(x = (-12).dp)
                            .clickable(
                                enabled = true,
                                onClick = {
                                    onLogLevelSelected(level)
                                }
                            )
                    ) {
                        RadioButton(
                            selected = currentLevel == level,
                            onClick = {
                                onLogLevelSelected(level)
                            }
                        )
                        Text(name, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    )
}

@Composable
fun ZipSaveAndShareDialog(
    title: String,
    includeDeviceInfo: Boolean,
    onIncludeDeviceInfoRequest: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        shape = RoundedCornerShape(32.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmRequest) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
        title = {
            Text(text = title)
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeDeviceInfo, onCheckedChange = onIncludeDeviceInfoRequest)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.include_device_info),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
fun PermissionDialog(
    onQuitAppRequest: () -> Unit,
    onCopyRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onQuitAppRequest,
        confirmButton = {
            TextButton(onClick = onQuitAppRequest) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCopyRequest) {
                Text(text = stringResource(id = android.R.string.copy))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(text = stringResource(id = R.string.grant_permissions))
        },
        text = {
            Text(text = stringResource(id = R.string.how_to_grant))
        }
    )
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun LogItem(
    modifier: Modifier = Modifier,
    item: LogData,
    onExpansionChanged: (Boolean) -> Unit
) {
    val textSize = item.textSize.toFloat()
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                enabled = true,
                onClick = {
                    onExpansionChanged(!item.isExpanded)
                },
            )
            .padding(vertical = 8.dp)
    ) {
        if (item.isExpanded && item.log is Log.Data) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.weight(.3f),
                    text = item.log.pid.toString(),
                    fontSize = TextUnit(textSize, TextUnitType.Sp)
                )
                Text(
                    modifier = Modifier.weight(.7f),
                    text = item.log.time.toString(),
                    fontSize = TextUnit(textSize, TextUnitType.Sp)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            if (item.log is Log.Data) {
                val color = getColorForLevel(
                    item.log.logLevel,
                    MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    modifier = Modifier.weight(.25f),
                    text = item.log.tag.toString(),
                    maxLines = if (item.isExpanded) Int.MAX_VALUE else 1,
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    overflow = TextOverflow.Ellipsis,
                    color = color
                )
                Text(
                    text = (item.log.logLevel ?: LogLevel.UNRECOGNIZED).name.first().toString(),
                    modifier = Modifier
                        .weight(.05f)
                        .padding(horizontal = 2.dp),
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    textAlign = TextAlign.Center,
                    color = color
                )
            }
            Text(
                modifier = Modifier.weight(if (item.log is Log.Divider) 1f else .7f),
                text = item.log.message,
                maxLines = if (item.isExpanded) Int.MAX_VALUE else 1,
                fontSize = TextUnit(textSize, TextUnitType.Sp),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getColorForLevel(level: LogLevel?, primaryColor: Color): Color =
    when (level) {
        LogLevel.VERBOSE, LogLevel.INFO, LogLevel.DEBUG -> primaryColor
        LogLevel.WARN -> androidx.compose.ui.graphics.lerp(primaryColor, Color.Red, 0.5f)
        LogLevel.ERROR, LogLevel.FATAL -> Color.Red
        else -> primaryColor
    }

@Preview
@Composable
fun PreviewLogItem() {
    LogItem(
        item = LogData(
            log = Log.Data(
                1000,
                "26-05 12:55:47",
                "AReallyLongTagForTesting",
                LogLevel.VERBOSE,
                "This is some log. More logs. More and more logs. Now something else. More logs."
            ),
            defaultExpanded = true,
            textSize = 12
        ),
        onExpansionChanged = {}
    )
}

sealed interface FABState {
    object Gone : FABState
    object Up : FABState
    object Down : FABState
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScrollFAB(
    fabState: FABState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = fabState !is FABState.Gone,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            AnimatedContent(targetState = fabState) {
                when (it) {
                    is FABState.Down -> {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(id = R.string.scroll_down_fab_content_desc),
                        )
                    }
                    is FABState.Up -> {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.scroll_up_fab_content_desc),
                        )
                    }
                    is FABState.Gone -> {}
                }
            }
        }
    }
}