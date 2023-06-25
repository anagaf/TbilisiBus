package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Route
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds

data class MapUiState(
    val inProgress: Boolean = false,
    val error: Error? = null,
    val cameraPosition: CameraPosition,
    val cameraBounds: LatLngBounds? = null,
    val route: Route? = null,
    val routeNumberDialogRequired: Boolean = false
) {
    enum class Error {
        RouteNotAvailable,
        LocationNotAvailable
    }
}
