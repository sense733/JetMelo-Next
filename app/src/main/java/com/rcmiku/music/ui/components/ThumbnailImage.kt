package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rcmiku.music.constants.GridThumbnailHeight
import com.rcmiku.music.constants.ListThumbnailSize
import com.rcmiku.music.constants.PlaylistThumbnailSize
import com.rcmiku.music.constants.ThumbnailCornerRadius

@Composable
fun ListThumbnailImage(url: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageRequest = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(144, 144)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .size(ListThumbnailSize)
    )
}

@Composable
fun GridThumbnailImage(url: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageRequest = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(384, 384)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .size(GridThumbnailHeight)
    )
}

@Composable
fun PlaylistThumbnailImage(url: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageRequest = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(600, 600)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .size(PlaylistThumbnailSize)
    )
}

@Composable
fun RadioThumbnailImage(url: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageRequest = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(600, 600)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(CircleShape)
            .size(PlaylistThumbnailSize)
    )
}