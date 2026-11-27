package com.rcmiku.music.utils

import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.compose.ui.unit.Dp
import com.rcmiku.ncmapi.utils.base64Encode
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID

private val deviceLock = Any()
@Volatile
private var cachedFallbackDeviceId: String? = null

fun getDeviceID(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("device_identity", android.content.Context.MODE_PRIVATE)
    var deviceId = prefs.getString("device_id", null)
    if (deviceId == null) {
        synchronized(deviceLock) {
            deviceId = prefs.getString("device_id", null)
            if (deviceId == null) {
                val uuid = UUID.randomUUID().toString().replace("-", "").take(16)
                val raw = "null 02:00:00:00:00:00 $uuid unknown"
                deviceId = urlEncode(base64Encode(raw.toByteArray()))
                prefs.edit().putString("device_id", deviceId).apply()
            }
        }
    }
    return deviceId!!
}

fun getDeviceID(): String {
    val cached = cachedFallbackDeviceId
    if (cached != null) return cached
    synchronized(deviceLock) {
        val existing = cachedFallbackDeviceId
        if (existing != null) return existing
        val uuid = UUID.randomUUID().toString().replace("-", "").take(16)
        val raw = "null 02:00:00:00:00:00 $uuid unknown"
        val generated = urlEncode(base64Encode(raw.toByteArray()))
        cachedFallbackDeviceId = generated
        return generated
    }
}

fun clearDeviceId(context: android.content.Context) {
    synchronized(deviceLock) {
        cachedFallbackDeviceId = null
        val prefs = context.getSharedPreferences("device_identity", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("device_id").apply()
    }
}

fun <T> getItemShape(
    prevItem: T?,
    nextItem: T?,
    corner: Dp,
    subCorner: Dp,
): ContinuousRoundedRectangle {
    val shape = when {
        prevItem != null && nextItem != null -> ContinuousRoundedRectangle(subCorner)
        prevItem == null && nextItem == null -> ContinuousRoundedRectangle(corner)
        prevItem == null -> ContinuousRoundedRectangle(
            topStart = corner, topEnd = corner,
            bottomStart = subCorner, bottomEnd = subCorner
        )

        else -> ContinuousRoundedRectangle(
            topStart = subCorner, topEnd = subCorner,
            bottomStart = corner, bottomEnd = corner
        )
    }

    return shape
}

fun formatPlayCount(count: Double): String {
    val locale = Locale.getDefault()
    return when {
        count >= 1_0000_0000 -> String.format(locale, "%.1f亿", count / 1_0000_0000.0)
        count >= 1_0000 -> String.format(locale, "%.1f万", count / 1_0000.0)
        else -> count.toInt().toString()
    }
}

fun formatTimestamp(timestamp: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.year}-${localDateTime.monthNumber}-${localDateTime.dayOfMonth}"
}


fun urlEncode(data: String): String = URLEncoder.encode(data, Charsets.UTF_8.name())

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}

data class YrcWord(
    val startMs: Long,
    val durationMs: Long,
    val startIndex: Int,
    val endIndex: Int
)

data class LrcLine(
    val time: Long,
    val text: String,
    val durationMs: Long = 0L,
    val words: List<YrcWord> = emptyList()
)

fun String.parseLrc(): List<LrcLine> {
    val regex = """\[(\d+):(\d+)\.(\d{2,3})]\s*(.*)""".toRegex()
    return this.lines().mapNotNull { it ->
        regex.matchEntire(it)?.destructured?.let { (min, sec, ms, text) ->
            val time = min.toLong() * 60 * 1000 + sec.toLong() * 1000 + ms.toLong().let {
                if (ms.length == 2) it * 10 else it
            }
            LrcLine(time, text)
        }
    }
}

fun String.parseYrc(): List<LrcLine> {
    val lineRegex = """^\[(\d+),(\d+)](.*)$""".toRegex()
    val wordRegex = """\((\d+),(\d+),\d+\)([^(]*)""".toRegex()
    val metaTimeRegex = """"t"\s*:\s*(\d+)""".toRegex()
    val metaTextRegex = """"tx"\s*:\s*"([^"]*)"""".toRegex()

    val parsed = this.lines().mapNotNull { rawLine ->
        val trimmed = rawLine.trim()
        when {
            trimmed.startsWith("{") -> {
                val time = metaTimeRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                val joinedText = metaTextRegex.findAll(trimmed)
                    .joinToString(separator = "") { it.groupValues[1] }
                    .trim()
                if (joinedText.isBlank()) null else LrcLine(time = time, text = joinedText)
            }
            else -> {
                val match = lineRegex.matchEntire(trimmed) ?: return@mapNotNull null
                val (lineStartStr, lineDurationStr, body) = match.destructured
                val lineStartMs = lineStartStr.toLongOrNull() ?: return@mapNotNull null
                val lineDurationMs = lineDurationStr.toLongOrNull() ?: 0L

                val textBuilder = StringBuilder()
                val words = mutableListOf<YrcWord>()
                for (wordMatch in wordRegex.findAll(body)) {
                    val (wordStartStr, wordDurStr, wordText) = wordMatch.destructured
                    val wordStartMs = wordStartStr.toLongOrNull() ?: continue
                    val wordDurationMs = wordDurStr.toLongOrNull() ?: 0L
                    val startIndex = textBuilder.length
                    textBuilder.append(wordText)
                    val endIndex = textBuilder.length
                    if (endIndex > startIndex) {
                        words.add(
                            YrcWord(
                                startMs = wordStartMs,
                                durationMs = wordDurationMs,
                                startIndex = startIndex,
                                endIndex = endIndex
                            )
                        )
                    }
                }
                val text = textBuilder.toString()
                if (text.isBlank() && words.isEmpty()) {
                    null
                } else {
                    LrcLine(
                        time = lineStartMs,
                        text = text,
                        durationMs = lineDurationMs,
                        words = words
                    )
                }
            }
        }
    }
    return parsed.sortedBy { it.time }
}