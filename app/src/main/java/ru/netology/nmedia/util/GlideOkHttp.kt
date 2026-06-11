package ru.netology.nmedia.util

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import ru.netology.nmedia.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object GlideOkHttp {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(15, 5, TimeUnit.MINUTES))
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 48
                    maxRequestsPerHost = 12
                },
            )
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(DebugLoggingInterceptor())
                }
                addInterceptor(RetryOnNetworkErrorInterceptor())
            }
            .build()
    }

    private class DebugLoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            val start = System.currentTimeMillis()
            ImageLoadLogger.logHttpStart(url, attempt = 1)
            return try {
                val response = chain.proceed(request)
                val bytes = response.body.contentLength()
                ImageLoadLogger.logHttpSuccess(
                    url = url,
                    code = response.code,
                    bytes = bytes,
                    durationMs = System.currentTimeMillis() - start,
                    attempt = 1,
                )
                response
            } catch (e: IOException) {
                ImageLoadLogger.logHttpError(
                    url = url,
                    attempt = 1,
                    durationMs = System.currentTimeMillis() - start,
                    error = e.message,
                )
                throw e
            }
        }
    }

    private class RetryOnNetworkErrorInterceptor(
        private val maxAttempts: Int = 2,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            var lastException: IOException? = null

            repeat(maxAttempts) { attempt ->
                if (chain.call().isCanceled()) {
                    throw IOException("Canceled")
                }
                val start = System.currentTimeMillis()
                if (BuildConfig.DEBUG && attempt > 0) {
                    ImageLoadLogger.logHttpStart(url, attempt = attempt + 1)
                }
                try {
                    val response = chain.proceed(request)
                    if (BuildConfig.DEBUG && attempt > 0) {
                        ImageLoadLogger.logHttpSuccess(
                            url = url,
                            code = response.code,
                            bytes = response.body.contentLength(),
                            durationMs = System.currentTimeMillis() - start,
                            attempt = attempt + 1,
                        )
                    }
                    return response
                } catch (e: IOException) {
                    if (chain.call().isCanceled() || e.message == "Canceled") {
                        if (BuildConfig.DEBUG) {
                            ImageLoadLogger.logHttpError(
                                url = url,
                                attempt = attempt + 1,
                                durationMs = System.currentTimeMillis() - start,
                                error = "Canceled",
                            )
                        }
                        throw e
                    }
                    lastException = e
                    if (BuildConfig.DEBUG) {
                        ImageLoadLogger.logHttpError(
                            url = url,
                            attempt = attempt + 1,
                            durationMs = System.currentTimeMillis() - start,
                            error = e.message,
                        )
                    }
                    if (attempt == maxAttempts - 1) {
                        throw e
                    }
                }
            }

            throw lastException ?: IOException("Request failed")
        }
    }
}
