package com.anagaf.tbilisibus.ui

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.Stop
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
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
    private val locationProvider: LocationProvider
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

        if (_uiState.value.routeNumber == null) {
            requestRouteNumber()
        } else {
            if (shouldRequestRouteNumber()) {
                requestRouteNumber()
            } else {
                onRouteNumberChosen(_uiState.value.routeNumber!!)
            }
        }
    }

    fun onCameraMove(pos: CameraPosition) {
        dataStore.lastCameraPosition = pos
        _uiState.update {
            it.copy(cameraPosition = pos, cameraBounds = null)
        }
    }

    @SuppressLint("MissingPermission")
    fun onMyLocationButtonClicked() {
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

    private fun makeBusMarker(bus: Bus): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Bus,
            bus.location.latLng,
            bus.direction,
            null
        )

    private fun makeStopMarker(stop: Stop): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Stop,
            stop.location.latLng,
            stop.direction,
            null
        )

//    private fun calculateBusHeading(bus: Bus, stops: Stops): Float? {
//        val nextStop = stops.items.find {
//            it.id == bus.nextStopId
//        }
//        if (nextStop == null) {
//            return null
//        }
//        return bus.location.getHeading(nextStop.location)
//    }

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
        if (_uiState.value.routeNumber != null) {
            retrieveRoute(_uiState.value.routeNumber!!)
        }
    }

    fun onZoomToShowRouteButtonClicked() {
        if (_uiState.value.routeMarkers.isNotEmpty()) {
            val markerBounds = LatLngBounds.builder().apply {
                _uiState.value.routeMarkers.forEach { marker: MapUiState.Marker ->
                    include(marker.location)
                }
            }.build()
            _uiState.update {
                it.copy(cameraBounds = markerBounds)
            }
        }
    }

    private fun shouldRequestRouteNumber(): Boolean {
        if (dataStore.lastRouteNumberRequestTimeInMillis == null) {
            return true
        }
        val millisSinceLastRequest = timeProvider.currentTimeMillis -
                dataStore.lastRouteNumberRequestTimeInMillis!!
        return millisSinceLastRequest > prefs.routeNumberTtl.inWholeMilliseconds
    }

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

                dataStore.lastRouteNumberRequestTimeInMillis = timeProvider.currentTimeMillis

                val busMarkers = route.buses.items.map {
                    makeBusMarker(it)
                }
                val stopMarkers = route.shape.stops.map {
                    makeStopMarker(it)
                }

                Timber.i("Route request succeed: ${busMarkers.size} buses, ${stopMarkers.size}")

                _uiState.update {
                    it.copy(
                        inProgress = false,
                        routeNumber = routeNumber,
                        routeMarkers = busMarkers + stopMarkers,
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