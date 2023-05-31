package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.google.android.gms.maps.model.LatLngBounds

sealed class MapUiState(
    val route: RouteUiState?
) {
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

    data class RouteUiState(val number: Int, val markers: List<Marker>)

    class Initial : MapUiState(null)

    class InProgress(uiState: MapUiState) : MapUiState(uiState.route)

    class Error(uiState: MapUiState, val errorMessage: String?) : MapUiState(uiState.route)

    class CameraMoveRequired(val cameraPosition: CameraPosition, uiState: MapUiState) :
        MapUiState(uiState.route)

    class CameraShowBoundsRequired(
        val bounds: LatLngBounds,
        uiState: MapUiState
    ) : MapUiState(uiState.route)

    class RouteNumberRequired(uiState: MapUiState) : MapUiState(uiState.route)

    class Idle(route: RouteUiState) : MapUiState(route)

}
