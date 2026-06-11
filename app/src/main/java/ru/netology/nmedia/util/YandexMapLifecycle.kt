package ru.netology.nmedia.util

import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView

object YandexMapLifecycle {

    private var mapKitStartedCount = 0

    fun startMap(mapView: MapView) {
        if (mapKitStartedCount == 0) {
            MapKitFactory.getInstance().onStart()
        }
        mapView.onStart()
        mapKitStartedCount++
    }

    fun stopMap(mapView: MapView) {
        mapView.onStop()
        mapKitStartedCount = (mapKitStartedCount - 1).coerceAtLeast(0)
        if (mapKitStartedCount == 0) {
            MapKitFactory.getInstance().onStop()
        }
    }
}
