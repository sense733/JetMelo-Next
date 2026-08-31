package com.rcmiku.music.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.LruCache
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
data class ArtworkColors(
    val dominantColor: Color = Color(0xFF1F1F22),
    val onDominantColor: Color = Color(0xFFE4E2E4),
    val accentColor: Color = Color(0xFFB6C7EA),
    val onAccentColor: Color = Color(0xFF1F314C),
    val surfaceScrim: Color = Color(0xDE100E14),
    val blurredBitmap: Bitmap? = null,
    val isDark: Boolean = true
)

val LocalArtworkColors = compositionLocalOf { ArtworkColors() }

object PaletteExtractor {
    private const val MAX_CACHE_SIZE = 60
    private const val SAMPLE_SIZE = 128
    private const val BLUR_RADIUS = 16

    private val cache = object : LruCache<String, ArtworkColors>(MAX_CACHE_SIZE) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: ArtworkColors?,
            newValue: ArtworkColors?
        ) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted && oldValue != newValue) {
                oldValue?.blurredBitmap?.recycle()
            }
        }
    }

    suspend fun extract(
        context: Context,
        artworkUri: Uri?,
        songId: String?,
        fallbackDominant: Color,
        fallbackAccent: Color
    ): ArtworkColors = withContext(Dispatchers.IO) {
        val cacheKey = songId ?: artworkUri?.toString()
        if (cacheKey == null) {
            return@withContext defaultArtworkColors(fallbackDominant, fallbackAccent)
        }

        cache.get(cacheKey)?.let { return@withContext it }

        if (artworkUri == null) {
            return@withContext defaultArtworkColors(fallbackDominant, fallbackAccent)
        }

        try {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(SAMPLE_SIZE, SAMPLE_SIZE)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = try {
                    result.image.toBitmap()
                } catch (_: Exception) {
                    val drawable = result.image.asDrawable(context.resources)
                    (drawable as? BitmapDrawable)?.bitmap
                }

                if (bitmap != null && !bitmap.isRecycled) {
                    val palette = Palette.from(bitmap).generate()
                    val dominantSwatch = palette.dominantSwatch
                        ?: palette.darkMutedSwatch
                        ?: palette.mutedSwatch
                    val vibrantSwatch = palette.vibrantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: dominantSwatch

                    val rawDominant = dominantSwatch?.rgb?.let { Color(it) } ?: fallbackDominant
                    val rawAccent = vibrantSwatch?.rgb?.let { Color(it) } ?: fallbackAccent

                    val adjustedDominant = ensureAccessibleBackground(rawDominant)
                    val onDominant = getAccessibleTextColor(adjustedDominant)

                    val adjustedAccent = ensureAccessibleAccent(rawAccent, adjustedDominant)
                    val onAccent = getAccessibleTextColor(adjustedAccent)

                    val blurred = try {
                        fastBlur(bitmap.copy(Bitmap.Config.ARGB_8888, true), BLUR_RADIUS)
                    } catch (_: Exception) {
                        null
                    }

                    val colors = ArtworkColors(
                        dominantColor = adjustedDominant,
                        onDominantColor = onDominant,
                        accentColor = adjustedAccent,
                        onAccentColor = onAccent,
                        surfaceScrim = Color(0xDE100E14),
                        blurredBitmap = blurred,
                        isDark = calculateLuminance(adjustedDominant) < 0.5f
                    )
                    cache.put(cacheKey, colors)
                    return@withContext colors
                }
            }
        } catch (_: Exception) {
        }

        val fallback = defaultArtworkColors(fallbackDominant, fallbackAccent)
        cache.put(cacheKey, fallback)
        fallback
    }

    private fun defaultArtworkColors(fallbackDominant: Color, fallbackAccent: Color): ArtworkColors {
        return ArtworkColors(
            dominantColor = fallbackDominant,
            onDominantColor = getAccessibleTextColor(fallbackDominant),
            accentColor = fallbackAccent,
            onAccentColor = getAccessibleTextColor(fallbackAccent)
        )
    }

    private fun calculateLuminance(color: Color): Float {
        fun channelLuminance(c: Float): Float {
            return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
        }
        return 0.2126f * channelLuminance(color.red) +
                0.7152f * channelLuminance(color.green) +
                0.0722f * channelLuminance(color.blue)
    }

    private fun calculateContrastRatio(fg: Color, bg: Color): Float {
        val l1 = calculateLuminance(fg)
        val l2 = calculateLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun getAccessibleTextColor(bg: Color): Color {
        val whiteContrast = calculateContrastRatio(Color.White, bg)
        val blackContrast = calculateContrastRatio(Color.Black, bg)
        return if (whiteContrast >= blackContrast) Color.White else Color.Black
    }

    private fun ensureAccessibleBackground(bg: Color): Color {
        var result = bg
        val whiteContrast = calculateContrastRatio(Color.White, result)
        val blackContrast = calculateContrastRatio(Color.Black, result)
        if (max(whiteContrast, blackContrast) < 4.5f) {
            val lum = calculateLuminance(result)
            result = if (lum < 0.5f) {
                Color(
                    red = result.red * 0.7f,
                    green = result.green * 0.7f,
                    blue = result.blue * 0.7f,
                    alpha = 1f
                )
            } else {
                Color(
                    red = min(1f, result.red * 1.3f),
                    green = min(1f, result.green * 1.3f),
                    blue = min(1f, result.blue * 1.3f),
                    alpha = 1f
                )
            }
        }
        return result
    }

    private fun ensureAccessibleAccent(accent: Color, background: Color): Color {
        var result = accent
        if (calculateContrastRatio(result, background) < 3.0f) {
            val bgLum = calculateLuminance(background)
            result = if (bgLum < 0.5f) {
                Color(
                    red = min(1f, result.red * 1.4f),
                    green = min(1f, result.green * 1.4f),
                    blue = min(1f, result.blue * 1.4f),
                    alpha = 1f
                )
            } else {
                Color(
                    red = result.red * 0.6f,
                    green = result.green * 0.6f,
                    blue = result.blue * 0.6f,
                    alpha = 1f
                )
            }
        }
        return result
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val w = sentBitmap.width
        val h = sentBitmap.height
        val pix = IntArray(w * h)
        sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pix[yi + min(wm, max(curI, 0))]
                sir = stack[curI + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(curI)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curY == 0) {
                    vmin[curX] = min(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }
        for (curX in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (curI in -radius..radius) {
                yi = max(0, yp) + curX
                sir = stack[curI + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(curI)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (curI < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curX == 0) {
                    vmin[curY] = min(curY + r1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return resultBitmap
    }
}

@Composable
fun rememberArtworkColors(
    artworkUri: Uri?,
    songId: String?,
    fallbackDominant: Color = MaterialTheme.colorScheme.surfaceContainer,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary
): ArtworkColors {
    val context = LocalContext.current
    var colors by remember(songId, artworkUri) {
        mutableStateOf(
            ArtworkColors(
                dominantColor = fallbackDominant,
                onDominantColor = Color.White,
                accentColor = fallbackAccent,
                onAccentColor = Color.White
            )
        )
    }

    LaunchedEffect(songId, artworkUri) {
        colors = PaletteExtractor.extract(
            context = context,
            artworkUri = artworkUri,
            songId = songId,
            fallbackDominant = fallbackDominant,
            fallbackAccent = fallbackAccent
        )
    }

    return colors
}
