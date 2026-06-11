package ru.netology.nmedia.util

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
object MediaDataSourceFactories {

    private const val API_HOST = "94.228.125.136"
    private const val USER_AGENT = "NMedia/1.0 (ExoPlayer)"

    fun forPlaybackUrl(
        context: Context,
        url: String,
        authenticatedClient: OkHttpClient,
    ): DataSource.Factory {
        val host = Uri.parse(url).host
        return if (host == API_HOST) {
            OkHttpDataSource.Factory(authenticatedClient)
                .setUserAgent(USER_AGENT)
        } else {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(900_000)
            DefaultDataSource.Factory(context, httpFactory)
        }
    }
}
