package ru.netology.nmedia.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.EntryPointAccessors
import java.io.InputStream

@GlideModule
class NMediaGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val okHttp = EntryPointAccessors.fromApplication(
            context,
            MediaNetworkingEntryPoint::class.java,
        ).okHttpClient()
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(okHttp),
        )
    }

    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) {
        val diskCacheSizeBytes = 250L * 1024 * 1024
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL),
        )
    }
}
