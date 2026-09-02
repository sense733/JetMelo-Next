package com.rcmiku.music.playback

import android.content.ComponentName
import android.content.Context
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

    private val sessionToken: SessionToken by lazy {
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    }

    fun init(context: Context) {
        if (!this::appContext.isInitialized) {
            appContext = context.applicationContext
        }

        if (controller != null || controllerFuture != null) return

        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    // 身份守卫：仅当仍是当前 future 时才赋值，防止 release() 后滞后回调
                    // 把已释放的实例写回单例
                    if (controllerFuture === future) {
                        controller = result
                    }
                }

                override fun onFailure(t: Throwable) {
                    MediaController.releaseFuture(future)
                    controllerFuture = null
                    controller = null
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun release() {
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
            controllerFuture = null
        }
        controller?.release()
        controller = null
    }
}

