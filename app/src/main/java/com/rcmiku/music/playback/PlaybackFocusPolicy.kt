package com.rcmiku.music.playback

import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player

internal object PlaybackFocusPolicy {

    private const val AUDIO_SOURCE_ECHO_REFERENCE = 1997
    private const val AUDIO_SOURCE_RADIO_TUNER = 1998
    private const val AUDIO_SOURCE_HOTWORD = 1999

    val musicAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    fun shouldHandleAudioFocus(allowMixWithOtherApps: Boolean): Boolean = !allowMixWithOtherApps

    fun isRelevantRecordingSource(audioSource: Int): Boolean {
        // Ignore system loopback/hotword capture so always-on wake word or internal screen capture does not falsely pause music.
        return audioSource != MediaRecorder.AudioSource.REMOTE_SUBMIX &&
            audioSource != AUDIO_SOURCE_ECHO_REFERENCE &&
            audioSource != AUDIO_SOURCE_RADIO_TUNER &&
            audioSource != AUDIO_SOURCE_HOTWORD
    }

    fun hasActiveExternalRecording(configs: List<AudioRecordingConfiguration>): Boolean {
        return configs.any { config ->
            val silenced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                config.isClientSilenced
            } else {
                false
            }
            !silenced && isRelevantRecordingSource(config.clientAudioSource)
        }
    }

    fun shouldPauseOnRecordingChange(
        allowMixWithOtherApps: Boolean,
        playWhenReady: Boolean,
        playbackSuppressionReason: Int,
        wasRecordingActive: Boolean,
        isRecordingActive: Boolean
    ): Boolean {
        if (allowMixWithOtherApps || !isRecordingActive || wasRecordingActive) return false
        // Skip explicit pause if ExoPlayer already suppressed playback for transient audio focus loss, preserving auto-resume.
        return playWhenReady && playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE
    }
}
