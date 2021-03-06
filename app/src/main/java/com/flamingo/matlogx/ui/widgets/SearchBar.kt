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

package com.flamingo.matlogx.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.flamingo.matlogx.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchBar(
    text: String,
    onSearchRequest: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onClearRecentQueryRequest: (String) -> Unit,
    onClearAllRecentQueriesRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    history: List<String> = emptyList(),
    maxHistoryCountToShow: Int = 10,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val interactions = interactionSource.interactions.collectAsState(null)
    var historyVisible by remember { mutableStateOf(true) }
    if (interactions.value is PressInteraction.Press) {
        historyVisible = true
    }
    ExposedDropdownMenuBox(expanded = history.isNotEmpty(), onExpandedChange = {}) {
        var searchText by remember { mutableStateOf(text) }
        TextField(
            modifier = modifier,
            value = searchText,
            placeholder = {
                if (hint != null) {
                    Text(text = hint)
                }
            },
            onValueChange = {
                searchText = it
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                Icon(
                    modifier = Modifier.clickable(
                        enabled = true,
                        onClick = {
                            onDismissRequest()
                            softwareKeyboardController?.hide()
                        },
                    ),
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.search_bar_close_icon_content_desc),
                )
            },
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchRequest(searchText)
                    historyVisible = false
                    softwareKeyboardController?.hide()
                },
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            singleLine = true,
            colors = colors,
            interactionSource = interactionSource
        )
        if (historyVisible) {
            ExposedDropdownMenu(
                expanded = history.isNotEmpty(),
                onDismissRequest = {},
            ) {
                history.filter {
                    it.contains(searchText, true)
                }.take(maxHistoryCountToShow)
                    .forEach {
                        RecentSearchRow(
                            text = it,
                            onClick = {
                                searchText = it
                                onSearchRequest(it)
                            },
                            onClearRecentQueryRequest = {
                                onClearRecentQueryRequest(it)
                            }
                        )
                    }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = true, onClick = onClearAllRecentQueriesRequest)
                        .padding(8.dp),
                    text = stringResource(id = R.string.clear),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RecentSearchRow(
    text: String,
    onClick: () -> Unit,
    onClearRecentQueryRequest: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(8.dp)
            .wrapContentSize(align = Alignment.Center)
            .clickable(
                enabled = true,
                onClick = onClick,
            )
    ) {
        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier.clickable(enabled = true, onClick = onClearRecentQueryRequest),
            imageVector = Icons.Filled.Clear,
            contentDescription = stringResource(id = R.string.clear_recent_search_button_content_desc)
        )
    }
}

@Preview
@Composable
fun PreviewRecentSearchRow() {
    RecentSearchRow(
        text = "Recent search",
        onClick = {},
        onClearRecentQueryRequest = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewSearchBar() {
    SearchBar(
        text = "",
        hint = "Hint",
        onSearchRequest = {},
        onDismissRequest = {},
        onClearRecentQueryRequest = {},
        onClearAllRecentQueriesRequest = {}
    )
}