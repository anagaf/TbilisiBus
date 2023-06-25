package com.anagaf.tbilisibus.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

data class Route(val number: Int, val forward: Elements, val backward: Elements) {
    data class Elements(
        val buses: List<LatLng>,
        val stops: List<LatLng>,
        val shapePoints: List<LatLng>
    )

    val bounds: LatLngBounds
        get() {
            val builder = LatLngBounds.builder()
            forward.shapePoints.forEach {
                builder.include(it)
            }
            backward.shapePoints.forEach {
                builder.include(it)
            }
            return builder.build()
        }
}
