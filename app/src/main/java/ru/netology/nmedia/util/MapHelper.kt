package ru.netology.nmedia.util

import android.content.Context
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectDragListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.IconStyle
import ru.netology.nmedia.dto.Coordinates

object MapHelper {
    val defaultLocation = Coordinates(lat = 55.751244, long = 37.618423)

    fun toPoint(coords: Coordinates): Point = Point(coords.lat, coords.long)

    fun fromPoint(point: Point): Coordinates =
        Coordinates(lat = point.latitude, long = point.longitude)

    fun moveCamera(mapView: MapView, coords: Coordinates, zoom: Float = 14f) {
        val map = mapView.mapWindow.map
        map.move(CameraPosition(toPoint(coords), zoom, 0f, 0f))
    }

    fun setupPickerMap(
        context: Context,
        mapView: MapView,
        initial: Coordinates,
        onCoordsChanged: (Coordinates) -> Unit,
    ): PlacemarkMapObject {
        val map = mapView.mapWindow.map
        val placemark = map.mapObjects.addPlacemark(toPoint(initial)).apply {
            setIcon(MapPinIcon.imageProvider(context))
            isDraggable = true
            setDragListener(object : MapObjectDragListener {
                override fun onMapObjectDragStart(mapObject: com.yandex.mapkit.map.MapObject) = Unit

                override fun onMapObjectDrag(mapObject: com.yandex.mapkit.map.MapObject, point: Point) {
                    onCoordsChanged(fromPoint(point))
                }

                override fun onMapObjectDragEnd(mapObject: com.yandex.mapkit.map.MapObject) {
                    val point = (mapObject as? PlacemarkMapObject)?.geometry ?: return
                    onCoordsChanged(fromPoint(point))
                }
            })
        }
        moveCamera(mapView, initial)

        map.addInputListener(object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                placemark.geometry = point
                onCoordsChanged(fromPoint(point))
                moveCamera(mapView, fromPoint(point))
            }

            override fun onMapLongTap(map: Map, point: Point) = Unit
        })

        return placemark
    }

    fun setupReadOnlyMap(context: Context, mapView: MapView, coords: Coordinates) {
        val map = mapView.mapWindow.map
        map.mapObjects.addPlacemark(toPoint(coords)).apply {
            setIcon(
                MapPinIcon.imageProvider(context),
                IconStyle().setScale(0.8f),
            )
        }
        moveCamera(mapView, coords)
    }
}
