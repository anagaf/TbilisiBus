package com.anagaf.tbilisibus.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Location(val lat: Double, val lon: Double) {

    /**
     * Returns a distance between this location and the other one.
     */
    fun getDistance(other: Location): Double {
        val x = (other.lon - lon) * kotlin.math.cos((lat + other.lat) / 2)
        val y = other.lat - lat
        return kotlin.math.sqrt(x * x + y * y) * 6371.0 // Earth's radius in kilometers
    }

    /**
     * Returns an angle in degrees between this location and the other one.
     */
    fun getHeading(other: Location): Float {
        val φ1 = Math.toRadians(lat)
        val φ2 = Math.toRadians(other.lat)
        val Δλ = Math.toRadians(other.lon - lon)

        val y = sin(Δλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) -
                sin(φ1) * cos(φ2) * cos(Δλ)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360 // normalize to 0-360 degrees

        return bearing.toFloat()
    }

    val latLng: com.google.android.gms.maps.model.LatLng
        get() = com.google.android.gms.maps.model.LatLng(lat, lon)
}