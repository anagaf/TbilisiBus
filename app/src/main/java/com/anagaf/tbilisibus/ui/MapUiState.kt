package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

data class MapUiState(
    val inProgress: Boolean = false,
    val error: Error? = null,
    val cameraPosition: CameraPosition,
    val cameraBounds: LatLngBounds? = null,
    val routeNumber: Int? = null,
    val routeMarkers: List<Marker> = emptyList(),
    val routeNumberDialogRequired: Boolean = false
) {
    enum class Error {
        RouteNotAvailable,
        LocationNotAvailable
    }

    data class Marker(
        val type: Type,
        val location: LatLng,
        val direction: Direction,
        val heading: Float?
    ) {

        enum class Type {
            Bus,
            Stop
        }
    }
}
