package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.ShapePoint
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

private val LatLng.x get() = latitude
private val LatLng.y get() = longitude

fun calculateBusHeading(busPos: LatLng, routeShape: List<ShapePoint>): Double? {
    var nextPoint: LatLng? = null
    var minDistance = Double.MAX_VALUE
    for (i in 0 until routeShape.size - 1) {
        val p1 = routeShape[i].position
        val p2 = routeShape[i + 1].position
        val v1 = LatLng(p1.x - p2.x, p1.y - p2.y)
        val v2 = LatLng(busPos.x - p2.x, busPos.y - p2.y)
        val angle = calculateAngle(v1, v2)
        if (kotlin.math.abs(angle) < 90) {
            val distance = SphericalUtil.computeDistanceBetween(busPos, p2)
            if (distance < minDistance) {
                minDistance = distance
                nextPoint = p2
            }
        }
    }
    return nextPoint?.let {
        SphericalUtil.computeHeading(busPos, it)
    }
}

private fun calculateAngle(vector1: LatLng, vector2: LatLng): Double {
    val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y
    val magnitude1 = kotlin.math.sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
    val magnitude2 = kotlin.math.sqrt(vector2.x * vector2.x + vector2.y * vector2.y)

    val angleCosine = dotProduct / (magnitude1 * magnitude2)
    return Math.toDegrees(kotlin.math.acos(angleCosine))
}