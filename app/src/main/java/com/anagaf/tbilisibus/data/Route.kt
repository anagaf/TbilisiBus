package com.anagaf.tbilisibus.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import timber.log.Timber

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
    Timber.d("Calculating bus heading: ${routeShape.size} points")
    for (i in 0 until routeShape.size - 1) {
        val p1 = routeShape[i].position
        val p2 = routeShape[i + 1].position
        val distance = calculateDistance(bus.position, p1, p2)
        Timber.d("-- distance: $distance")
        if (distance < minDistance) {
            minDistance = distance
            nextPoint = p2
            Timber.d("-- new min distance: $minDistance")
        }
    }
    return nextPoint?.let {
        SphericalUtil.computeHeading(bus.position, it)
    }
}

private val LatLng.x get() = latitude
private val LatLng.y get() = longitude

private fun calculateProjection(p0: LatLng, p1: LatLng, p2: LatLng): LatLng {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val A = dy
    val B = -dx
    val C = dx * p1.y - dy * p1.x
    val A_perpendicular = -B
    val B_perpendicular = A
    val C_perpendicular = -A_perpendicular * p0.x - B_perpendicular * p0.y
    val x_projection =
        (B * C_perpendicular - B_perpendicular * C) / (A * B_perpendicular - A_perpendicular * B)
    val y_projection =
        (A_perpendicular * C - A * C_perpendicular) / (A * B_perpendicular - A_perpendicular * B)
    return LatLng(x_projection, y_projection)
}

private fun calculateDistance(p0: LatLng, p1: LatLng, p2: LatLng): Double {
    val projection = calculateProjection(p0, p1, p2)
    return SphericalUtil.computeDistanceBetween(p0, projection)
}

//private fun calculateProjection(p0: LatLng, p1: LatLng, p2: LatLng): LatLng {
//    val dx = p2.x - p1.x
//    val dy = p2.y - p1.y
//    val A = dy
//    val B = -dx
//    val C = dx * p1.y - dy * p1.x
//    val A_perpendicular = -B
//    val B_perpendicular = A
//    val C_perpendicular = -A_perpendicular * p0.x - B_perpendicular * p0.y
//    val x_projection =
//        (B * C_perpendicular - B_perpendicular * C) / (A * B_perpendicular - A_perpendicular * B)
//    val y_projection =
//        (A_perpendicular * C - A * C_perpendicular) / (A * B_perpendicular - A_perpendicular * B)
//    return LatLng(x_projection, y_projection)
//}
//
//private fun calculateDistance(p0: LatLng, p1: LatLng, p2: LatLng): Double {
//    val projection = calculateProjection(p0, p1, p2)
//    return SphericalUtil.computeDistanceBetween(p0, projection)
//}





