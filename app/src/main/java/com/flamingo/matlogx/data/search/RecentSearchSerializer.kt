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

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore

import com.google.protobuf.InvalidProtocolBufferException

import java.io.InputStream
import java.io.OutputStream

object RecentSearchSerializer : Serializer<RecentSearch> {

    override val defaultValue: RecentSearch = RecentSearch.newBuilder().build()

    override suspend fun readFrom(input: InputStream): RecentSearch {
        try {
            return RecentSearch.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot parse recent search proto", exception)
        }
    }

    override suspend fun writeTo(
        t: RecentSearch,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.recentSearchDataStore: DataStore<RecentSearch> by dataStore(
    fileName = "recent_search",
    serializer = RecentSearchSerializer
)