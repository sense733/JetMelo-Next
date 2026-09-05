package com.rcmiku.music.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.rcmiku.music.SearchHistory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object SearchHistorySerializer : Serializer<SearchHistory> {

    private const val TAG = "SearchHistorySerializer"

    override val defaultValue: SearchHistory = SearchHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SearchHistory {
        return try {
            SearchHistory.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            Log.e(TAG, "Failed to parse SearchHistory protobuf, fallback to default", e)
            defaultValue
        } catch (e: IOException) {
            Log.e(TAG, "I/O error reading SearchHistory protobuf, fallback to default", e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: SearchHistory, output: OutputStream) = t.writeTo(output)
}

val Context.searchHistoryDataStore: DataStore<SearchHistory> by dataStore(
    fileName = "search_history.pb",
    serializer = SearchHistorySerializer
)