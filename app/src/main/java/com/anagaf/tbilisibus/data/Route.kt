package com.anagaf.tbilisibus.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class Bus(val position: LatLng)

data class Stop(val position: LatLng)

data class ShapePoint(val position: LatLng)

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

fun calculateBusHeading(bus: Bus, routeShape: List<ShapePoint>): Double? {
    var nextPoint: LatLng? = null
    var minDistance = Double.MAX_VALUE
    for (i in 0 until routeShape.size - 1) {
        val p1 = routeShape[i].position
        val p2 = routeShape[i + 1].position
        val distance = calculateDistance(bus.position, p1, p2)
        if (distance < minDistance) {
            minDistance = distance
            nextPoint = p2
        }
    }
    return nextPoint?.let {
        SphericalUtil.computeHeading(bus.position, it)
    }
}

private val LatLng.x get() = latitude
private val LatLng.y get() = longitude

private fun calculateDistance(p0: LatLng, p1: LatLng, p2: LatLng): Double =
    abs((p2.y - p1.y) * p0.x - (p2.x - p1.x) * p0.y + p2.x * p1.y - p2.y * p1.x) /
            sqrt((p2.y - p1.y).pow(2.0) + (p2.x - p1.x).pow(2.0))



