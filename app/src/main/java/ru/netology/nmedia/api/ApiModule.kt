package ru.netology.nmedia.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.auth.AppAuth
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ApiModule {

    companion object {
        private const val BASE_URL = "http://94.228.125.136:8080/api/"
        private const val API_HOST = "94.228.125.136"

        private fun needsApiAuth(host: String): Boolean = host == API_HOST
    }

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
    }

    @Singleton
    @Provides
    fun provideOkHttp(
        logging: HttpLoggingInterceptor,
        appAuth: AppAuth,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder()
            if (needsApiAuth(request.url.host)) {
                val token = appAuth.authState.value?.token
                val apiKey = BuildConfig.NMEDIA_API_KEY
                if (apiKey.isNotBlank()) {
                    builder.addHeader("Api-Key", apiKey)
                }
                if (token != null) {
                    builder.addHeader("Authorization", token)
                }
            }
            chain.proceed(builder.build())
        }
        .addInterceptor(logging)
        .build()

    @Singleton
    @Provides
    @PlaybackClient
    fun providePlaybackOkHttp(appAuth: AppAuth): OkHttpClient =
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                if (needsApiAuth(request.url.host)) {
                    val token = appAuth.authState.value?.token
                    val apiKey = BuildConfig.NMEDIA_API_KEY
                    if (apiKey.isNotBlank()) {
                        builder.addHeader("Api-Key", apiKey)
                    }
                    if (token != null) {
                        builder.addHeader("Authorization", token)
                    }
                }
                chain.proceed(builder.build())
            }
            .build()

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Singleton
    @Provides
    fun provideApiService(
        retrofit: Retrofit
    ): PostApiService = retrofit.create()

    @Singleton
    @Provides
    fun provideEventApiService(
        retrofit: Retrofit,
    ): EventApiService = retrofit.create()
}