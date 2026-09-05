package com.rcmiku.music.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.rcmiku.music.FavoriteSongIds
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FavoriteSongIdsSerializer : Serializer<FavoriteSongIds> {
    private const val TAG = "FavoriteSongIdsSerializer"

    override val defaultValue: FavoriteSongIds
        get() = FavoriteSongIds.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FavoriteSongIds {
        return try {
            FavoriteSongIds.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            Log.e(TAG, "Failed to parse FavoriteSongIds protobuf, fallback to default", exception)
            defaultValue
        } catch (e: IOException) {
            Log.e(TAG, "I/O error reading FavoriteSongIds protobuf, fallback to default", e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: FavoriteSongIds, output: OutputStream) = t.writeTo(output)

}

val Context.favoriteSongIdsDatastore: DataStore<FavoriteSongIds> by dataStore(
    fileName = "favorite_song_ids.pb",
    serializer = FavoriteSongIdsSerializer
)