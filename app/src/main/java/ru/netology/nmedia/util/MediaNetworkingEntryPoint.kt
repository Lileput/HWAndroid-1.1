package ru.netology.nmedia.util

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaNetworkingEntryPoint {
    fun okHttpClient(): OkHttpClient
}
