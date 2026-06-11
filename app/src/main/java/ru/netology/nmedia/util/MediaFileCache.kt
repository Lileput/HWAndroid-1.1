package ru.netology.nmedia.util

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import ru.netology.nmedia.BuildConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object MediaFileCache {

    private const val TAG = "MediaFileCache"
    private const val CACHE_DIR = "media_playback"
    private const val API_HOST = "94.228.125.136"
    private const val MIN_VIDEO_BYTES = 100_000L
    private const val MIN_AUDIO_BYTES = 8_000L

    private val cdnDownloadClient: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.MINUTES)
        .writeTimeout(15, TimeUnit.MINUTES)
        .callTimeout(16, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    fun getOrDownload(
        context: Context,
        url: String,
        authenticatedClient: OkHttpClient,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null,
    ): File? {
        val target = cacheFileFor(context, url)
        if (target.exists() && isValidFile(target, url, expectedLength = null)) {
            log("cache hit ${target.length()} bytes")
            onProgress?.invoke(target.length(), target.length())
            return target
        }
        if (target.exists()) {
            target.delete()
        }
        target.parentFile?.mkdirs()

        val client = downloadClientFor(url, authenticatedClient)
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .build()
        return try {
            log("download start $url")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log("download http ${response.code}")
                    return null
                }
                val contentType = response.header("Content-Type").orEmpty()
                if (isBadContentType(contentType)) {
                    log("download bad type $contentType")
                    return null
                }
                val body = response.body ?: return null
                val expected = body.contentLength()
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress?.invoke(downloaded, expected)
                        }
                    }
                }
                if (!isValidFile(target, url, expected)) {
                    log(
                        "download invalid size=${target.length()} expected=$expected type=$contentType",
                    )
                    target.delete()
                    return null
                }
                log("download ok ${target.length()} bytes type=$contentType")
                target
            }
        } catch (e: IOException) {
            log("download fail ${e.message}")
            target.delete()
            null
        }
    }

    fun isImageKitUrl(url: String): Boolean = url.contains("ik.imagekit.io")

    fun clearCached(context: Context, url: String) {
        val file = cacheFileFor(context, url)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun isBadContentType(contentType: String): Boolean =
        contentType.contains("text/html", ignoreCase = true) ||
            contentType.contains("application/json", ignoreCase = true) ||
            contentType.contains("text/plain", ignoreCase = true)

    private fun isValidFile(file: File, url: String, expectedLength: Long?): Boolean {
        val size = file.length()
        val minBytes = if (ImageUrlResolver.isAudioUrl(url)) MIN_AUDIO_BYTES else MIN_VIDEO_BYTES
        if (size < minBytes) return false
        if (expectedLength != null && expectedLength > 0) {
            if (size < expectedLength * 9 / 10) return false
        }
        if (ImageUrlResolver.isVideoUrl(url) || url.contains(".mp4", ignoreCase = true)) {
            return hasMp4Header(file)
        }
        return true
    }

    private fun hasMp4Header(file: File): Boolean =
        try {
            file.inputStream().use { input ->
                val header = ByteArray(12)
                val read = input.read(header)
                if (read < 8) return false
                String(header, 4, 4, Charsets.US_ASCII) == "ftyp"
            }
        } catch (_: IOException) {
            false
        }

    private fun downloadClientFor(url: String, authenticatedClient: OkHttpClient): OkHttpClient {
        if (Uri.parse(url).host != API_HOST) {
            return cdnDownloadClient
        }
        return authenticatedClient.newBuilder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.MINUTES)
            .writeTimeout(15, TimeUnit.MINUTES)
            .callTimeout(16, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun cacheFileFor(context: Context, url: String): File {
        val extension = url.substringAfterLast('.', "mp4").substringBefore('?')
        val safeExt = extension.take(8).ifBlank { "mp4" }
        val name = "${url.hashCode().toUInt().toString(16)}.$safeExt"
        return File(File(context.cacheDir, CACHE_DIR), name)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
