package com.rcmiku.music.playback

import android.media.MediaRecorder
import androidx.media3.common.C
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFocusPolicyTest {

    @Test
    fun `shouldHandleAudioFocus is enabled only when mixing is disabled`() {
        assertTrue(PlaybackFocusPolicy.shouldHandleAudioFocus(allowMixWithOtherApps = false))
        assertFalse(PlaybackFocusPolicy.shouldHandleAudioFocus(allowMixWithOtherApps = true))
    }

    @Test
    fun `musicAudioAttributes uses media usage and music content type`() {
        assertEquals(C.USAGE_MEDIA, PlaybackFocusPolicy.musicAudioAttributes.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, PlaybackFocusPolicy.musicAudioAttributes.contentType)
    }

    @Test
    fun `isRelevantRecordingSource accepts mic sources and ignores hotword or loopback`() {
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.MIC))
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.DEFAULT))
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.CAMCORDER))
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.VOICE_RECOGNITION))
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION))
        assertTrue(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.UNPROCESSED))

        assertFalse(PlaybackFocusPolicy.isRelevantRecordingSource(MediaRecorder.AudioSource.REMOTE_SUBMIX))
        assertFalse(PlaybackFocusPolicy.isRelevantRecordingSource(1997))
        assertFalse(PlaybackFocusPolicy.isRelevantRecordingSource(1998))
        assertFalse(PlaybackFocusPolicy.isRelevantRecordingSource(1999))
    }

    @Test
    fun `shouldPauseOnRecordingChange only pauses on new active recording when mixing disabled and playing`() {
        assertTrue(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = false,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                wasRecordingActive = false,
                isRecordingActive = true
            )
        )

        assertFalse(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = true,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                wasRecordingActive = false,
                isRecordingActive = true
            )
        )

        assertFalse(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = false,
                playWhenReady = false,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                wasRecordingActive = false,
                isRecordingActive = true
            )
        )

        assertFalse(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = false,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS,
                wasRecordingActive = false,
                isRecordingActive = true
            )
        )

        assertFalse(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = false,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                wasRecordingActive = true,
                isRecordingActive = true
            )
        )

        assertFalse(
            PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = false,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                wasRecordingActive = true,
                isRecordingActive = false
            )
        )
    }
}
