package com.rcmiku.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
object PlayerController {
    private lateinit var appContext: Context
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller by mutableStateOf<MediaController?>(null)
        private set

    private fun createSessionToken(): SessionToken {
        return SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    }

    fun init(context: Context) {
        appContext = context.applicationContext

        if (controller?.isConnected == true) return
        if (controllerFuture != null) return
        if (controller != null) {
            runCatching { controller?.release() }
            controller = null
        }

        val future = MediaController.Builder(appContext, createSessionToken())
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
        controllerFuture = future
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    if (controllerFuture === future) {
                        controller = result
                    }
                }

                override fun onFailure(t: Throwable) {
                    MediaController.releaseFuture(future)
                    if (controllerFuture === future) {
                        controllerFuture = null
                        controller = null
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun release() {
        val future = controllerFuture
        val currentController = controller
        controllerFuture = null
        controller = null

        runCatching {
            if (future != null) {
                MediaController.releaseFuture(future)
            } else {
                currentController?.release()
            }
        }
    }
}

