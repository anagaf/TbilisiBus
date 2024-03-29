package com.anagaf.tbilisibus.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

data class Bus(val position: LatLng)

data class Stop(val position: LatLng)

data class ShapePoint(val position: LatLng)

typealias BusesByDirection = Map<Direction, List<Bus>>
typealias StopsByDirection = Map<Direction, List<Stop>>
typealias ShapePointsByDirection = Map<Direction, List<ShapePoint>>

data class RouteInfo(val stops: StopsByDirection, val shapePoints: ShapePointsByDirection)

data class Route(val number: Int, val forward: Elements, val backward: Elements) {
    data class Elements(
        val buses: List<Bus>,
        val stops: List<Stop>,
        val shapePoints: List<ShapePoint>
    )

    val bounds: LatLngBounds
        get() {
            val builder = LatLngBounds.builder()
            forward.shapePoints.forEach {
                builder.include(it.position)
            }
            backward.shapePoints.forEach {
                builder.include(it.position)
            }
            return builder.build()
        }
}


