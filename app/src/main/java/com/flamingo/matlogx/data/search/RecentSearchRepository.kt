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

package com.flamingo.matlogx.data.search

import android.content.Context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecentSearchRepository(context: Context) {

    private val recentSearch = context.recentSearchDataStore

    val recentSearchList: Flow<List<String>> =
        recentSearch.data.map { list -> list.recentSearchList.map { it.searchQuery } }

    suspend fun saveRecentSearchQuery(query: String) {
        recentSearch.updateData {
            it.toBuilder()
                .addRecentSearch(
                    RecentSearchData.newBuilder()
                        .setSearchQuery(query)
                        .setTimestamp(System.currentTimeMillis())
                        .build()
                )
                .build()
        }
    }

    suspend fun clearRecentSearchQuery(query: String) {
        recentSearch.updateData { data ->
            val index =
                data.recentSearchList.indexOfFirst { it.searchQuery == query }.takeIf { it >= 0 }
                    ?: return@updateData data
            data.toBuilder()
                .removeRecentSearch(index)
                .build()
        }
    }

    suspend fun clearAllRecentSearchQueries() {
        recentSearch.updateData {
            it.toBuilder()
                .clearRecentSearch()
                .build()
        }
    }
}