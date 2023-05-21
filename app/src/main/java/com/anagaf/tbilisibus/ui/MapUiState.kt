package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.google.android.gms.maps.model.LatLngBounds

sealed class MapUiState(val route: RouteUiState?) {

    data class Marker(
        val type: Type,
        val location: Location,
        val title: String,
        val direction: Direction,
        val heading: Float?
    ) {

        enum class Type {
            Bus,
            Stop
        }
    }

    data class RouteUiState(val routeNumber: Int, val markers: List<Marker>)

    class InProgress(route: RouteUiState?) : MapUiState(route)

    class Error(val message: String, route: RouteUiState?) : MapUiState(route)

    class CameraMoveRequired(val cameraPosition: CameraPosition, route: RouteUiState?) : MapUiState(route)

    class CameraShowBoundsRequired(val bounds: LatLngBounds, route: RouteUiState?) : MapUiState(route)

    class RouteAvailable(route: RouteUiState): MapUiState(route)
}
