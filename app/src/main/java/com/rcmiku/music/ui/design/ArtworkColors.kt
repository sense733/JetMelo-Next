package com.rcmiku.music.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun computeWashVerticalFeatherAlpha(yFraction: Float): Float {
    val y = yFraction.coerceIn(0f, 1f)
    return when {
        y <= 0.26f -> 1f - (y / 0.26f) * 0.15f
        y <= 0.46f -> 0.85f - ((y - 0.26f) / 0.20f) * 0.49f
        y <= 0.64f -> 0.36f * (1f - (y - 0.46f) / 0.18f)
        else -> 0f
    }.coerceIn(0f, 1f)
}

@Immutable
data class ArtworkColors(
    val dominantColor: Color = Color(0xFF1F1F22),
    val onDominantColor: Color = Color(0xFFE4E2E4),
    val accentColor: Color = Color(0xFFB6C7EA),
    val onAccentColor: Color = Color(0xFF1F314C),
    val ambientColor: Color = dominantColor,
    val surfaceScrim: Color = Color(0xDE100E14),
    val blurredBitmap: Bitmap? = null,
    val isDark: Boolean = true
)

val LocalArtworkColors = compositionLocalOf { ArtworkColors() }

object PaletteExtractor {
    private const val MAX_CACHE_SIZE = 120
    private const val MAX_WASH_CACHE_SIZE = 16
    private const val SAMPLE_SIZE = 48
    private const val WASH_SAMPLE_SIZE = 64
    private const val BLUR_RADIUS = 16
    private const val WASH_BLUR_RADIUS = 10

    private const val MIN_BACKGROUND_CONTRAST = 4.5f
    private const val MIN_ACCENT_CONTRAST = 3.0f
    private const val MAX_CONTRAST_ITERATIONS = 12
    private const val MAX_COLOR_DRIFT_SQ = 0.18f

    private fun colorDistance(a: Color, b: Color): Float {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return dr * dr + dg * dg + db * db
    }

    private val cache = LruCache<String, ArtworkColors>(MAX_CACHE_SIZE)
    private val blurredWashCache = LruCache<String, ImageBitmap>(MAX_WASH_CACHE_SIZE)
    private val preloadSemaphore = Semaphore(2)

    private fun washCacheKey(artworkUri: Any?, songId: String?): String? {
        return artworkUri?.toString()?.takeIf { it.isNotEmpty() }
            ?: songId?.takeIf { it.isNotEmpty() }
    }

    fun getCachedBlurredWash(artworkUri: Any?, songId: String?): ImageBitmap? {
        val key = washCacheKey(artworkUri, songId) ?: return null
        return blurredWashCache.get(key)
    }

    suspend fun getOrCreateBlurredWash(
        context: Context,
        artworkUri: Any?,
        songId: String?
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = washCacheKey(artworkUri, songId) ?: return@withContext null
        blurredWashCache.get(key)?.let { return@withContext it }
        if (artworkUri == null) return@withContext null

        try {
            val imageLoader = SingletonImageLoader.get(context)
            val coverKey = songId?.let { "cover_$it" }
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(WASH_SAMPLE_SIZE, WASH_SAMPLE_SIZE)
                .apply {
                    if (coverKey != null) {
                        placeholderMemoryCacheKey(coverKey)
                    }
                }
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = try {
                    result.image.toBitmap()
                } catch (_: Exception) {
                    val drawable = result.image.asDrawable(context.resources)
                    (drawable as? BitmapDrawable)?.bitmap
                }

                if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                    val washImage = withContext(Dispatchers.Default) {
                        val cropped = if (bitmap.width >= 20 && bitmap.height >= 20) {
                            val insetX = bitmap.width / 10
                            val insetY = bitmap.height / 10
                            Bitmap.createBitmap(
                                bitmap,
                                insetX,
                                insetY,
                                bitmap.width - insetX * 2,
                                bitmap.height - insetY * 2
                            )
                        } else {
                            bitmap
                        }
                        val blurred = fastBlur(cropped, WASH_BLUR_RADIUS)
                        if (cropped !== bitmap) {
                            cropped.recycle()
                        }
                        bakeVerticalFeatherAlpha(blurred).asImageBitmap()
                    }
                    blurredWashCache.put(key, washImage)
                    return@withContext washImage
                }
            }
        } catch (e: Exception) {
            Log.w("PaletteExtractor", "Failed to create blurred wash for $key", e)
        }
        null
    }

    private fun bakeVerticalFeatherAlpha(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return bitmap
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (y in 0 until h) {
            val yFraction = if (h > 1) y.toFloat() / (h - 1) else 0f
            val feather = computeWashVerticalFeatherAlpha(yFraction)
            val rowStart = y * w
            for (x in 0 until w) {
                val idx = rowStart + x
                val px = pixels[idx]
                val origAlpha = (px ushr 24) and 0xFF
                val outAlpha = (origAlpha * feather).roundToInt().coerceIn(0, 255)
                pixels[idx] = (outAlpha shl 24) or (px and 0x00FFFFFF)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun getCached(
        artworkUri: Any?,
        songId: String?,
        fallbackDominant: Color,
        fallbackAccent: Color
    ): ArtworkColors? {
        val fallbackSuffix = "${fallbackDominant.toArgb()},${fallbackAccent.toArgb()}"
        val baseKey = songId ?: artworkUri?.toString()
        val cacheKey = if (baseKey.isNullOrEmpty()) "" else "$baseKey|$fallbackSuffix"
        if (cacheKey.isEmpty()) return null
        return cache.get(cacheKey)
    }

    suspend fun preload(
        context: Context,
        artworkUri: Any?,
        songId: String?,
        fallbackDominant: Color,
        fallbackAccent: Color
    ) {
        extract(context, artworkUri, songId, fallbackDominant, fallbackAccent)
    }

    suspend fun preloadBatch(
        context: Context,
        items: List<Pair<Any?, String?>>,
        fallbackDominant: Color,
        fallbackAccent: Color
    ) = withContext(Dispatchers.IO) {
        items.forEach { (artworkUri, songId) ->
            if (artworkUri != null) {
                launch {
                    preloadSemaphore.withPermit {
                        preload(context, artworkUri, songId, fallbackDominant, fallbackAccent)
                    }
                }
            }
        }
    }

    suspend fun extract(
        context: Context,
        artworkUri: Any?,
        songId: String?,
        fallbackDominant: Color,
        fallbackAccent: Color
    ): ArtworkColors = withContext(Dispatchers.IO) {
        val fallbackSuffix = "${fallbackDominant.toArgb()},${fallbackAccent.toArgb()}"
        val baseKey = songId ?: artworkUri?.toString()
        val cacheKey = if (baseKey.isNullOrEmpty()) "" else "$baseKey|$fallbackSuffix"
        if (cacheKey.isEmpty()) {
            return@withContext defaultArtworkColors(fallbackDominant, fallbackAccent)
        }

        cache.get(cacheKey)?.let { return@withContext it }

        if (artworkUri == null) {
            return@withContext defaultArtworkColors(fallbackDominant, fallbackAccent)
        }

        try {
            val imageLoader = SingletonImageLoader.get(context)
            val coverKey = songId?.let { "cover_$it" }
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(SAMPLE_SIZE, SAMPLE_SIZE)
                .apply {
                    if (coverKey != null) {
                        placeholderMemoryCacheKey(coverKey)
                    }
                }
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
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
                    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
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
                            fastBlur(bitmap, BLUR_RADIUS)
                        } catch (e: Exception) {
                            Log.w("PaletteExtractor", "fastBlur failed", e)
                            null
                        }
                    } else {
                        null
                    }

                    val ambient = extractAmbientColor(palette, adjustedDominant)

                    val colors = ArtworkColors(
                        dominantColor = adjustedDominant,
                        onDominantColor = onDominant,
                        accentColor = adjustedAccent,
                        onAccentColor = onAccent,
                        ambientColor = ambient,
                        surfaceScrim = Color(0xDE100E14),
                        blurredBitmap = null,
                        isDark = calculateLuminance(adjustedDominant) < 0.5f
                    )
                    cache.put(cacheKey, colors)
                    return@withContext colors.copy(blurredBitmap = blurred)
                }
            }
        } catch (e: Exception) {
            Log.w("PaletteExtractor", "Failed to extract palette for $cacheKey", e)
        }

        defaultArtworkColors(fallbackDominant, fallbackAccent)
    }

    private fun defaultArtworkColors(fallbackDominant: Color, fallbackAccent: Color): ArtworkColors {
        return ArtworkColors(
            dominantColor = fallbackDominant,
            onDominantColor = getAccessibleTextColor(fallbackDominant),
            accentColor = fallbackAccent,
            onAccentColor = getAccessibleTextColor(fallbackAccent),
            ambientColor = fallbackDominant
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

    fun getAccessibleTextColor(bg: Color): Color {
        val whiteContrast = calculateContrastRatio(Color.White, bg)
        val blackContrast = calculateContrastRatio(Color.Black, bg)
        return if (whiteContrast >= blackContrast) Color.White else Color.Black
    }

    /**
     * Resolves an accent color guaranteed to meet minimum graphical contrast against [background],
     * falling back to [fallback] if no candidate satisfies contrast within drift bounds.
     */
    fun ensureAccessibleAccent(
        candidate: Color,
        background: Color,
        fallback: Color
    ): Color = findAccessibleAccent(listOf(candidate), background, fallback)

    /**
     * Refines a raw extracted accent into a restrained, clean tonal accent paired with
     * crisp icon contrast on both light and dark ambient surfaces.
     */
    fun refinePlayerAccent(
        candidate: Color,
        ambientFallback: Color,
        background: Color,
        isDarkTheme: Boolean,
        fallback: Color
    ): Color {
        val source = when {
            candidate.alpha > 0f -> candidate
            ambientFallback.alpha > 0f -> ambientFallback
            else -> return fallback
        }
        val candidateHsl = rgbToHsl(source)
        val ambientHsl = if (ambientFallback.alpha > 0f) rgbToHsl(ambientFallback) else candidateHsl
        val baseHsl = if (candidateHsl[1] < 0.06f && ambientHsl[1] >= 0.06f) {
            ambientHsl
        } else {
            candidateHsl
        }
        val h = baseHsl[0]
        val s = baseHsl[1]
        val l = baseHsl[2]

        if (s < 0.06f) {
            return if (isDarkTheme) Color(0xFFE6E6EA) else Color(0xFF2C2C30)
        }

        if (isDarkTheme) {
            val targetS = s.coerceIn(0.30f, 0.48f)
            var targetL = l.coerceIn(0.74f, 0.82f)
            var refined = hslToColor(h, targetS, targetL)
            while (calculateContrastRatio(refined, background) < 3.0f && targetL < 0.88f) {
                targetL = min(0.88f, targetL + 0.02f)
                refined = hslToColor(h, targetS, targetL)
            }
            return refined
        } else {
            val targetS = s.coerceIn(0.28f, 0.46f)
            var targetL = l.coerceIn(0.34f, 0.43f)
            var refined = hslToColor(h, targetS, targetL)
            // Keep light-mode accent low-glare while ensuring white play/pause glyphs remain crisp.
            while ((calculateContrastRatio(Color.White, refined) < 4.6f ||
                        calculateContrastRatio(refined, background) < 3.0f) &&
                targetL > 0.26f
            ) {
                targetL = max(0.26f, targetL - 0.02f)
                refined = hslToColor(h, targetS, targetL)
            }
            return if (refined.alpha > 0f) refined else fallback
        }
    }

    private fun findAccessibleBackground(
        candidates: List<Color>,
        fallback: Color
    ): Color {
        val pool = (candidates + fallback).distinct()
        for (candidate in pool) {
            val origin = candidate
            var current = candidate
            for (step in 0 until MAX_CONTRAST_ITERATIONS) {
                val whiteContrast = calculateContrastRatio(Color.White, current)
                val blackContrast = calculateContrastRatio(Color.Black, current)
                if (max(whiteContrast, blackContrast) >= MIN_BACKGROUND_CONTRAST) {
                    if (colorDistance(current, origin) <= MAX_COLOR_DRIFT_SQ) return current else break
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
            val origin = candidate
            var current = candidate
            for (step in 0 until MAX_CONTRAST_ITERATIONS) {
                if (calculateContrastRatio(current, background) >= MIN_ACCENT_CONTRAST) {
                    if (colorDistance(current, origin) <= MAX_COLOR_DRIFT_SQ) return current else break
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

    private fun extractAmbientColor(palette: Palette, fallback: Color): Color {
        val candidates = palette.swatches.filter { it.population > 0 }
        if (candidates.isNotEmpty()) {
            val totalPopulation = candidates.sumOf { it.population }.toFloat().coerceAtLeast(1f)
            var weightedAvgS = 0f
            var weightedAvgL = 0f
            var vividPopulation = 0
            for (swatch in candidates) {
                val hsl = rgbToHsl(Color(swatch.rgb))
                val share = swatch.population / totalPopulation
                weightedAvgS += hsl[1] * share
                weightedAvgL += hsl[2] * share
                if (hsl[1] >= 0.20f) {
                    vividPopulation += swatch.population
                }
            }
            val vividShare = vividPopulation / totalPopulation
            if (weightedAvgS < 0.07f && vividShare < 0.08f) {
                return hslToColor(h = 0f, s = 0f, l = weightedAvgL.coerceIn(0.42f, 0.56f))
            }

            val substantial = candidates.filter { it.population / totalPopulation >= 0.02f }
                .ifEmpty { candidates }
            val chosen = substantial.maxByOrNull { swatch ->
                val hsl = rgbToHsl(Color(swatch.rgb))
                val s = hsl[1]
                val l = hsl[2]
                val popShare = (swatch.population / totalPopulation).coerceAtLeast(1e-4f)
                val lightnessWeight = (1f - kotlin.math.abs(l - 0.5f) * 1.4f).coerceAtLeast(0.15f)
                (s.coerceAtMost(0.75f) + 0.12f) * popShare.pow(0.65f) * lightnessWeight
            }?.let { Color(it.rgb) } ?: fallback

            val hsl = rgbToHsl(chosen)
            val h = hsl[0]
            val s = hsl[1]
            val l = hsl[2]
            return if (s >= 0.06f) {
                val normalizedS = if (s < 0.45f) {
                    (s * 1.2f).coerceIn(0.42f, 0.62f)
                } else {
                    s.coerceAtMost(0.68f)
                }
                hslToColor(
                    h = h,
                    s = normalizedS,
                    l = l.coerceIn(0.48f, 0.66f)
                )
            } else {
                hslToColor(
                    h = h,
                    s = s,
                    l = l.coerceIn(0.42f, 0.56f)
                )
            }
        }

        val fallbackPicked = (palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch)
            ?.let { Color(it.rgb) } ?: fallback
        val hsl = rgbToHsl(fallbackPicked)
        return if (hsl[1] >= 0.06f) {
            hslToColor(
                h = hsl[0],
                s = hsl[1].coerceIn(0.42f, 0.68f),
                l = hsl[2].coerceIn(0.48f, 0.66f)
            )
        } else {
            hslToColor(h = hsl[0], s = hsl[1], l = hsl[2].coerceIn(0.42f, 0.56f))
        }
    }

    private fun rgbToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val d = maxC - minC
        val l = (maxC + minC) * 0.5f
        if (d <= 1e-6f) {
            return floatArrayOf(0f, 0f, l)
        }
        val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
        val hRaw = when (maxC) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        return floatArrayOf((hRaw * 60f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        if (s <= 1e-6f) {
            val v = l.coerceIn(0f, 1f)
            return Color(red = v, green = v, blue = v, alpha = 1f)
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        val hk = ((h % 360f) + 360f) % 360f / 360f
        fun hue2rgb(tRaw: Float): Float {
            var t = tRaw
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }
        return Color(
            red = hue2rgb(hk + 1f / 3f).coerceIn(0f, 1f),
            green = hue2rgb(hk).coerceIn(0f, 1f),
            blue = hue2rgb(hk - 1f / 3f).coerceIn(0f, 1f),
            alpha = 1f
        )
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
    artworkUri: Any?,
    songId: String?,
    fallbackDominant: Color = MaterialTheme.colorScheme.surfaceContainer,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary
): ArtworkColors {
    val context = LocalContext.current
    var colors by remember(songId, artworkUri, fallbackDominant, fallbackAccent) {
        val cached = PaletteExtractor.getCached(
            artworkUri = artworkUri,
            songId = songId,
            fallbackDominant = fallbackDominant,
            fallbackAccent = fallbackAccent
        )
        mutableStateOf(
            cached ?: ArtworkColors(
                dominantColor = fallbackDominant,
                onDominantColor = PaletteExtractor.getAccessibleTextColor(fallbackDominant),
                accentColor = fallbackAccent,
                onAccentColor = PaletteExtractor.getAccessibleTextColor(fallbackAccent)
            )
        )
    }

    LaunchedEffect(songId, artworkUri, fallbackDominant, fallbackAccent) {
        if (PaletteExtractor.getCached(artworkUri, songId, fallbackDominant, fallbackAccent) == null) {
            colors = PaletteExtractor.extract(
                context = context,
                artworkUri = artworkUri,
                songId = songId,
                fallbackDominant = fallbackDominant,
                fallbackAccent = fallbackAccent
            )
        }
    }

    return colors
}

@Immutable
data class PlayerContentColors(
    val primaryContent: Color,
    val secondaryContent: Color,
    val mutedContent: Color,
    val trackInactive: Color,
    val accent: Color,
    val onAccent: Color,
    val activeItemContainer: Color
)

/**
 * Derives theme-adaptive foreground and accent colors for player surfaces based on the current
 * Material theme luminance and [artworkColors].
 */
@Composable
fun rememberPlayerContentColors(
    artworkColors: ArtworkColors = LocalArtworkColors.current
): PlayerContentColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    val surfaceRef = if (isDarkTheme) colorScheme.background else colorScheme.surface
    val primary = if (isDarkTheme) Color.White else Color(0xFF1A1A1E)
    val accent = remember(
        artworkColors.ambientColor,
        surfaceRef,
        isDarkTheme,
        colorScheme.primary
    ) {
        PaletteExtractor.refinePlayerAccent(
            candidate = artworkColors.ambientColor,
            ambientFallback = artworkColors.ambientColor,
            background = surfaceRef,
            isDarkTheme = isDarkTheme,
            fallback = colorScheme.primary
        )
    }
    val onAccent = remember(accent, isDarkTheme) {
        if (isDarkTheme) {
            Color(0xFF18181C)
        } else if (accent.luminance() < 0.50f) {
            Color.White
        } else {
            PaletteExtractor.getAccessibleTextColor(accent)
        }
    }
    return remember(isDarkTheme, primary, accent, onAccent) {
        PlayerContentColors(
            primaryContent = primary,
            secondaryContent = primary.copy(alpha = if (isDarkTheme) 0.72f else 0.68f),
            mutedContent = primary.copy(alpha = if (isDarkTheme) 0.45f else 0.42f),
            trackInactive = primary.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
            accent = accent,
            onAccent = onAccent,
            activeItemContainer = accent.copy(alpha = if (isDarkTheme) 0.20f else 0.14f)
        )
    }
}
