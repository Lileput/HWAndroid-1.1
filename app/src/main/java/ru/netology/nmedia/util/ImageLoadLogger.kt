package ru.netology.nmedia.util

import android.util.Log
import ru.netology.nmedia.BuildConfig

object ImageLoadLogger {
    const val TAG = "NMediaImage"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun logLoadStart(
        kind: String,
        original: String,
        url: String,
        widthPx: Int,
        heightPx: Int,
        viewInfo: String,
    ) {
        d(
            "START $kind | ${widthPx}x$heightPx | view=$viewInfo | " +
                "original=${original.take(120)} | url=${url.take(160)}",
        )
    }

    fun logLoadSuccess(kind: String, url: String, durationMs: Long) {
        d("OK $kind | ${durationMs}ms | url=${url.take(160)}")
    }

    fun logLoadFailed(kind: String, url: String, durationMs: Long, error: String?, canceled: Boolean) {
        val status = if (canceled) "CANCELED" else "FAIL"
        d("$status $kind | ${durationMs}ms | error=$error | url=${url.take(160)}")
    }

    fun logHttpStart(url: String, attempt: Int) {
        d("HTTP -> attempt=$attempt | $url")
    }

    fun logHttpSuccess(url: String, code: Int, bytes: Long, durationMs: Long, attempt: Int) {
        d("HTTP OK | attempt=$attempt | $code | ${bytes}B | ${durationMs}ms | $url")
    }

    fun logHttpError(url: String, attempt: Int, durationMs: Long, error: String?) {
        d("HTTP ERR | attempt=$attempt | ${durationMs}ms | error=$error | $url")
    }
}
