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

package com.flamingo.matlogx.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

import com.flamingo.matlogx.R
import com.flamingo.matlogx.data.settings.DEFAULT_BUFFERS
import com.flamingo.matlogx.ui.states.SettingsScreenState
import com.flamingo.matlogx.ui.states.rememberSettingsScreenState
import com.flamingo.support.compose.ui.preferences.CheckBoxPreference
import com.flamingo.support.compose.ui.preferences.EditTextPreference
import com.flamingo.support.compose.ui.preferences.Entry
import com.flamingo.support.compose.ui.preferences.ListPreference
import com.flamingo.support.compose.ui.preferences.MultiSelectListPreference
import com.flamingo.support.compose.ui.preferences.PreferenceGroupHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    state: SettingsScreenState = rememberSettingsScreenState()
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            val containerColor = MaterialTheme.colorScheme.primary
            val contentColorForContainer = contentColorFor(backgroundColor = containerColor)
            SmallTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button_content_desc),
                            tint = contentColorForContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = contentColorForContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 12.dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            item {
                PreferenceGroupHeader(title = stringResource(id = R.string.appearance))
            }
            item {
                val textSize by state.textSize.collectAsState(initial = 0)
                ListPreference(
                    title = stringResource(id = R.string.text_size),
                    entries = listOf(
                        Entry(stringResource(id = R.string.small), 8),
                        Entry(stringResource(id = R.string.medium), 12),
                        Entry(stringResource(id = R.string.large), 16),
                    ),
                    value = textSize,
                    onEntrySelected = {
                        state.setTextSize(it)
                    }
                )
            }
            item {
                val expanded by state.expandedByDefault.collectAsState(initial = false)
                CheckBoxPreference(
                    title = stringResource(id = R.string.expand_logs_by_default),
                    summary = stringResource(id = R.string.expand_logs_summary),
                    checked = expanded,
                    onCheckedChange = {
                        state.setExpandedByDefault(it)
                    }
                )
            }
            item {
                PreferenceGroupHeader(title = stringResource(id = R.string.configuration))
            }
            item {
                val selectedBuffers by state.logcatBuffers.collectAsState(initial = DEFAULT_BUFFERS)
                MultiSelectListPreference(
                    title = stringResource(id = R.string.log_buffers),
                    summary = selectedBuffers.map { it.name.lowercase() }.joinToString(","),
                    entries = state.logcatBufferEntries,
                    values = selectedBuffers,
                    onValuesUpdated = {
                        state.setLogcatBuffers(it)
                    },
                    onDismissListener = {}
                )
            }
            item {
                val limit by state.logcatSizeLimit.collectAsState(0)
                EditTextPreference(
                    title = stringResource(id = R.string.log_display_limit),
                    summary = if (limit == 0)
                        stringResource(id = R.string.log_display_limit_summary_no_limit)
                    else
                        stringResource(
                            id = R.string.log_display_limit_summary_placeholder,
                            limit
                        ),
                    value = limit.toString(),
                    onValueSelected = {
                        state.setLogcatSizeLimit(it.toInt())
                    }
                )
            }
        }
    }
}