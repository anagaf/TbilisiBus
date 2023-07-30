package com.anagaf.tbilisibus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.RouteProvider
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val routeProvider: RouteProvider,
    private val dataStore: AppDataStore,
    private val prefs: Preferences,
    private val timeProvider: TimeProvider,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    companion object {
        val kInitialCameraPosition = CameraPosition.Builder()
            .target(LatLng(41.7225, 44.7925)) // Tbilisi center
            .zoom(12f)
            .build()
    }

    private val _uiState = MutableStateFlow(
        value = MapUiState(
            cameraPosition = kInitialCameraPosition
        )
    )

    val uiState = _uiState.asStateFlow()

    fun onMapReady() {
        if (dataStore.lastCameraPosition != null) {
            _uiState.update { it.copy(cameraPosition = dataStore.lastCameraPosition!!) }
        }

        if (_uiState.value.route == null) {
            requestRouteNumber()
        } else {
            if (shouldRequestRouteNumber()) {
                requestRouteNumber()
            } else {
                onRouteNumberChosen(_uiState.value.route!!.number)
            }
        }
    }

    fun onCameraMove(pos: CameraPosition) {
        dataStore.lastCameraPosition = pos
        _uiState.update {
            it.copy(cameraPosition = pos, cameraBounds = null)
        }
    }

    fun onMyLocationButtonClicked() {
        moveCameraToCurrentLocation()
    }

    private fun moveCameraToCurrentLocation() {
        Timber.d("Moving camera to current location")

        viewModelScope.launch {
            _uiState.update {
                it.copy(inProgress = true)
            }
            try {
                val location = locationProvider.getLastLocation()
                val zoom = _uiState.value.cameraPosition.zoom
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        cameraPosition = CameraPosition.Builder(it.cameraPosition)
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(zoom)
                            .build()
                    )
                }
            } catch (ex: Exception) {
                Timber.e("User location is not available: ${ex.message}")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        error = MapUiState.Error.LocationNotAvailable,
                    )
                }
            }
        }
    }

    fun onRouteNumberChosen(routeNumber: Int) {
        _uiState.update {
            it.copy(routeNumberDialogRequired = false)
        }
        retrieveRoute(routeNumber)
    }

    fun onRouteNumberChangeDismissed() {
        _uiState.update {
            it.copy(routeNumberDialogRequired = false)
        }
    }

    fun onReloadRouteButtonClicked() {
        if (_uiState.value.route != null) {
            retrieveRoute(_uiState.value.route!!.number)
        }
    }

    fun onZoomToShowRouteButtonClicked() {
        _uiState.value.route?.let {
            _uiState.update {
                it.copy(cameraBounds = it.route!!.bounds)
            }
        }
    }

    private fun shouldRequestRouteNumber(): Boolean =
        dataStore.lastRouteNumberRequestTime?.let { time ->
            timeProvider.now - time > prefs.routeNumberTtl
        } ?: true

    private fun requestRouteNumber() {
        _uiState.update {
            it.copy(routeNumberDialogRequired = true)
        }
    }

    private fun retrieveRoute(routeNumber: Int) {

        viewModelScope.launch {
            _uiState.update {
                it.copy(inProgress = true)
            }

            try {
                val route = routeProvider.getRoute(routeNumber)

                dataStore.lastRouteNumberRequestTime = timeProvider.now

                _uiState.update {
                    it.copy(
                        inProgress = false,
                        route = route,
                    )
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Route request failed")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        // TODO: take message from resources
                        //errorMessage = "Route request failed: ${ex.message}"
                        error = MapUiState.Error.RouteNotAvailable
                    )
                }
            }
        }
    }

    fun onChooseRouteButtonClicked() {
        _uiState.update {
            it.copy(routeNumberDialogRequired = true)
        }
    }

    fun onErrorMessageShown() {
        _uiState.update {
            it.copy(error = null)
        }
    }
}