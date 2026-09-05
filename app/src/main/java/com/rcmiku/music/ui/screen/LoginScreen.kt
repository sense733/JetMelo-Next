package com.rcmiku.music.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.utils.getDeviceID
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.json
import com.rcmiku.ncmapi.utils.parseCookieString


@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {

    var ncmCookie by rememberPreference(ncmCookieKey, "")
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoBack() == true)
                                webView?.goBack()
                            else
                                navController.navigateUp()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                val uri = url?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
                                val isWhitelistedHost = uri != null && uri.scheme == "https" &&
                                    (uri.host == "music.163.com" || uri.host == "y.music.163.com")
                                if (isWhitelistedHost && uri?.path?.startsWith("/m") == true) {
                                    val cookie = CookieManager.getInstance().getCookie(url) ?: return
                                    if (cookie.isBlank()) return
                                    val parsedCookie = parseCookieString(cookie)
                                    val hasSession = parsedCookie.containsKey("MUSIC_U") || parsedCookie.containsKey("MUSIC_A")
                                    if (!hasSession) return

                                    val allowedAuthKeys = setOf(
                                        "MUSIC_U", "MUSIC_A", "__csrf", "__remember_me"
                                    )
                                    val cookieMap = parsedCookie.filterKeys { it in allowedAuthKeys }.toMutableMap()
                                    val currentContext = view?.context ?: navController.context
                                    cookieMap[CookieKeys.DEVICE_ID] = getDeviceID(currentContext)
                                    cookieMap[CookieKeys.OS_VER] = Build.VERSION.RELEASE
                                    cookieMap[CookieKeys.MOBILE_NAME] = Build.MODEL
                                    ncmCookie = json.encodeToString(cookieMap)
                                    
                                    settings.javaScriptEnabled = false
                                    clearCache(true)
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            allowFileAccess = false
                            allowContentAccess = false
                            savePassword = false
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webView = this
                        loadUrl("https://music.163.com/m/login")
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.clearHistory()
                    view.removeAllViews()
                    view.destroy()
                    webView = null
                }
            )
        }
    }
}


