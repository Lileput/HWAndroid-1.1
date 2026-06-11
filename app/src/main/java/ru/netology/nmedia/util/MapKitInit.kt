package ru.netology.nmedia.util

import android.content.Context
import android.util.Log
import com.yandex.mapkit.MapKitFactory
import ru.netology.nmedia.BuildConfig

object MapKitInit {

    private const val TAG = "NMediaMap"

    @Volatile
    private var initialized = false

    fun setApiKeyOnce() {
        val key = BuildConfig.MAPKIT_API_KEY.trim()
        if (key.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "MAPKIT_API_KEY is empty — add it to local.properties and Sync Gradle")
            }
            return
        }
        if (key.startsWith("\"") || key.endsWith("\"")) {
            Log.e(TAG, "MAPKIT_API_KEY must not be wrapped in quotes in local.properties")
        }
        MapKitFactory.setApiKey(key)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "MapKit API key set (length=${key.length})")
        }
    }

    fun isApiKeyConfigured(): Boolean = BuildConfig.MAPKIT_API_KEY.trim().isNotBlank()

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            MapKitFactory.initialize(context.applicationContext)
            initialized = true
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "MapKitFactory.initialize() done")
            }
        }
    }
}
