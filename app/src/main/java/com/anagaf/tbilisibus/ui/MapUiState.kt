package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Route
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds

data class MapUiState(
    val alignment: UiAlignment,
    val inProgress: Boolean = false,
    val error: Error? = null,
    val cameraPosition: CameraPosition,
    val cameraBounds: LatLngBounds? = null,
    val route: Route? = null,
    val dialogRequired: Dialog? = null
) {
    enum class Error {
        RouteNotAvailable,
        LocationNotAvailable
    }

    enum class Dialog {
        Route,
        About,
        OutOfTbilisi
    }
}
