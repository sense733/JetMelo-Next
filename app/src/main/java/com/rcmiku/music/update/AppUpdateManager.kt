package com.rcmiku.music.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * 应用内自更新状态。
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val versionName: String) : UpdateState
    data class Available(val info: AppUpdateInfo, val showDialog: Boolean = true) : UpdateState
    data class Downloading(
        val info: AppUpdateInfo,
        val progress: Float?,
        val showDialog: Boolean = true
    ) : UpdateState
    data class ReadyToInstall(
        val info: AppUpdateInfo,
        val apkFile: File,
        val needsInstallPermission: Boolean = false,
        val showDialog: Boolean = true
    ) : UpdateState
    data class Error(
        val message: String,
        val info: AppUpdateInfo? = null,
        val isDownloadError: Boolean = false,
        val showDialog: Boolean = true
    ) : UpdateState
}

/**
 * 负责检查远程版本、下载 APK 安装包并拉起系统安装器。
 */
object AppUpdateManager {

    const val UPDATE_MANIFEST_URL = "http://120.76.137.248:8080/update/version.json"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var appContext: Context? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun checkForUpdate(isManual: Boolean) {
        val context = appContext ?: return
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) {
            if (isManual && current is UpdateState.Downloading) {
                _state.value = current.copy(showDialog = true)
            }
            return
        }
        if (isManual && current is UpdateState.ReadyToInstall) {
            _state.value = current.copy(
                needsInstallPermission = !canRequestPackageInstalls(context),
                showDialog = true
            )
            return
        }

        if (isManual) {
            _state.value = UpdateState.Checking
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchRemoteInfo() }
            result.onSuccess { remote ->
                val localCode = currentVersionCode(context)
                if (remote.versionCode > localCode) {
                    _state.value = UpdateState.Available(info = remote, showDialog = true)
                } else {
                    _state.value = if (isManual) {
                        UpdateState.UpToDate(currentVersionName(context))
                    } else {
                        UpdateState.Idle
                    }
                }
            }.onFailure { error ->
                _state.value = if (isManual) {
                    UpdateState.Error(
                        message = error.message ?: "网络请求失败",
                        isDownloadError = false,
                        showDialog = true
                    )
                } else {
                    UpdateState.Idle
                }
            }
        }
    }

    fun startDownload(info: AppUpdateInfo) {
        val context = appContext ?: return
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(info = info, progress = null, showDialog = true)

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                downloadApkFile(context, info) { progress ->
                    val current = _state.value
                    if (current is UpdateState.Downloading) {
                        _state.value = current.copy(progress = progress)
                    }
                }
            }

            result.onSuccess { file ->
                val needsPermission = !canRequestPackageInstalls(context)
                _state.value = UpdateState.ReadyToInstall(
                    info = info,
                    apkFile = file,
                    needsInstallPermission = needsPermission,
                    showDialog = true
                )
                if (!needsPermission) {
                    launchApkInstaller(context, file)
                }
            }.onFailure { error ->
                _state.value = UpdateState.Error(
                    message = error.message ?: "下载失败",
                    info = info,
                    isDownloadError = true,
                    showDialog = true
                )
            }
        }
    }

    fun installPendingApk() {
        val context = appContext ?: return
        val current = _state.value as? UpdateState.ReadyToInstall ?: return
        if (!canRequestPackageInstalls(context)) {
            _state.value = current.copy(needsInstallPermission = true)
            openUnknownSourcesSettings(context)
            return
        }
        _state.value = current.copy(needsInstallPermission = false)
        launchApkInstaller(context, current.apkFile)
    }

    fun dismissDialog() {
        when (val current = _state.value) {
            is UpdateState.Available -> {
                if (!current.info.forceUpdate) {
                    _state.value = current.copy(showDialog = false)
                }
            }
            is UpdateState.Downloading -> {
                _state.value = current.copy(showDialog = false)
            }
            is UpdateState.ReadyToInstall -> {
                if (!current.info.forceUpdate) {
                    _state.value = current.copy(showDialog = false)
                }
            }
            is UpdateState.Error -> {
                _state.value = current.copy(showDialog = false)
            }
            else -> Unit
        }
    }

    fun showDialogIfPending(context: Context) {
        init(context)
        when (val current = _state.value) {
            is UpdateState.Available -> _state.value = current.copy(showDialog = true)
            is UpdateState.Downloading -> _state.value = current.copy(showDialog = true)
            is UpdateState.ReadyToInstall -> _state.value = current.copy(
                needsInstallPermission = !canRequestPackageInstalls(context),
                showDialog = true
            )
            else -> checkForUpdate(isManual = true)
        }
    }

    private suspend fun fetchRemoteInfo(): Result<AppUpdateInfo> = runCatching {
        val connection = (URL(UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<AppUpdateInfo>(body)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadApkFile(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (Float?) -> Unit
    ): Result<File> = runCatching {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }

        val targetFile = File(updatesDir, "jetmelo-update-${info.versionCode}.apk")
        val tempFile = File(updatesDir, "jetmelo-update-${info.versionCode}.apk.tmp")

        val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            val totalBytes = connection.contentLengthLong
            val digest = if (info.sha256.isNotBlank()) MessageDigest.getInstance("SHA-256") else null

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastReportedProgress = -1f
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest?.update(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            val progress = (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            if (progress - lastReportedProgress >= 0.01f || progress >= 1f) {
                                lastReportedProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            if (digest != null) {
                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualSha256.equals(info.sha256.trim(), ignoreCase = true)) {
                    tempFile.delete()
                    error("安装包校验失败")
                }
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile
        } finally {
            connection.disconnect()
        }
    }

    private fun canRequestPackageInstalls(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    private fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun currentVersionCode(context: Context): Long {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return PackageInfoCompat.getLongVersionCode(info)
    }

    private fun currentVersionName(context: Context): String {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName.orEmpty()
    }
}
