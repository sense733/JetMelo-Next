package com.rcmiku.music.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
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

    private const val MIN_BACKGROUND_CONTRAST = 4.5f
    private const val MIN_ACCENT_CONTRAST = 3.0f
    private const val MAX_CONTRAST_ITERATIONS = 12

    private val cache = LruCache<String, ArtworkColors>(MAX_CACHE_SIZE)

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
                    val dominantCandidates = listOfNotNull(
                        palette.dominantSwatch,
                        palette.darkMutedSwatch,
                        palette.mutedSwatch,
                        palette.darkVibrantSwatch,
                        palette.lightMutedSwatch
                    ).map { Color(it.rgb) }

                    val accentCandidates = listOfNotNull(
                        palette.vibrantSwatch,
                        palette.lightVibrantSwatch,
                        palette.darkVibrantSwatch,
                        palette.dominantSwatch
                    ).map { Color(it.rgb) }

                    val adjustedDominant = findAccessibleBackground(dominantCandidates, fallbackDominant)
                    val onDominant = getAccessibleTextColor(adjustedDominant)

                    val adjustedAccent = findAccessibleAccent(accentCandidates, adjustedDominant, fallbackAccent)
                    val onAccent = getAccessibleTextColor(adjustedAccent)

                    val blurred = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        try {
                            fastBlur(bitmap.copy(Bitmap.Config.ARGB_8888, true), BLUR_RADIUS)
                        } catch (_: Exception) {
                            null
                        }
                    } else {
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

    private fun findAccessibleBackground(
        candidates: List<Color>,
        fallback: Color
    ): Color {
        val pool = (candidates + fallback).distinct()
        for (candidate in pool) {
            var current = candidate
            for (step in 0 until MAX_CONTRAST_ITERATIONS) {
                val whiteContrast = calculateContrastRatio(Color.White, current)
                val blackContrast = calculateContrastRatio(Color.Black, current)
                if (max(whiteContrast, blackContrast) >= MIN_BACKGROUND_CONTRAST) {
                    return current
                }
                val lum = calculateLuminance(current)
                current = if (lum < 0.5f) {
                    val nextR = current.red * 0.9f
                    val nextG = current.green * 0.9f
                    val nextB = current.blue * 0.9f
                    if (nextR < 0.05f && nextG < 0.05f && nextB < 0.05f) break
                    Color(red = nextR, green = nextG, blue = nextB, alpha = 1f)
                } else {
                    val nextR = min(1f, current.red * 1.1f + 0.02f)
                    val nextG = min(1f, current.green * 1.1f + 0.02f)
                    val nextB = min(1f, current.blue * 1.1f + 0.02f)
                    if (nextR > 0.95f && nextG > 0.95f && nextB > 0.95f) break
                    Color(red = nextR, green = nextG, blue = nextB, alpha = 1f)
                }
            }
        }
        return pool.firstOrNull() ?: fallback
    }

    // Accent contrast ≥3.0:1 is strictly targeted for pure icons/graphical objects (WCAG 2.1 AA UI component requirement); any text-bearing accent elements must satisfy ≥4.5:1.
    private fun findAccessibleAccent(
        candidates: List<Color>,
        background: Color,
        fallback: Color
    ): Color {
        val pool = (candidates + fallback).distinct()
        val bgLum = calculateLuminance(background)
        for (candidate in pool) {
            var current = candidate
            for (step in 0 until MAX_CONTRAST_ITERATIONS) {
                if (calculateContrastRatio(current, background) >= MIN_ACCENT_CONTRAST) {
                    return current
                }
                current = if (bgLum < 0.5f) {
                    val nextR = min(1f, current.red * 1.1f + 0.02f)
                    val nextG = min(1f, current.green * 1.1f + 0.02f)
                    val nextB = min(1f, current.blue * 1.1f + 0.02f)
                    if (nextR >= 0.98f && nextG >= 0.98f && nextB >= 0.98f) break
                    Color(red = nextR, green = nextG, blue = nextB, alpha = 1f)
                } else {
                    val nextR = current.red * 0.88f
                    val nextG = current.green * 0.88f
                    val nextB = current.blue * 0.88f
                    if (nextR < 0.05f && nextG < 0.05f && nextB < 0.05f) break
                    Color(red = nextR, green = nextG, blue = nextB, alpha = 1f)
                }
            }
        }
        return pool.firstOrNull() ?: fallback
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
