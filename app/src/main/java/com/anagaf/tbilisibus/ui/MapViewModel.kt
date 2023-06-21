package com.anagaf.tbilisibus.ui

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val routeProvider: RouteProvider,
    private val dataStore: AppDataStore,
    private val prefs: Preferences,
    private val timeProvider: TimeProvider,
    private val locationProvider: LocationProvider
) : AndroidViewModel(app) {

    companion object {
        val INITIAL_LAT_LNG = LatLng(41.7225, 44.7925) // Tbilisi center
        const val INITIAL_ZOOM = 12f
    }

    private val _uiState = MutableStateFlow<MapUiState>(
        value = MapUiState(
            inProgress = false,
            cameraPosition = initialCameraPosition
        )
    )

    val uiState = _uiState.asStateFlow()


    fun onMapReady() {
        if (_uiState.value.routeNumber == null) {
            requestRouteNumber()
        } else {
            if (shouldRequestRouteNumber()) {
                requestRouteNumber()
            } else {
                onRouteNumberChangeConfirmed(_uiState.value.routeNumber!!)
            }
        }
    }

    fun onCameraMove(pos: CameraPosition) {
        Log.d(TAG, "Updating view model camera position: $pos")

        dataStore.lastMapPosition = pos
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
                Log.d(
                    "MapViewModel",
                    "Prev state camera position is ${_uiState.value.cameraPosition}"
                )
                val location = locationProvider.getLastLocation()
                Log.d("MapViewModel", "User location is $location")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        cameraPosition = CameraPosition.Builder(it.cameraPosition)
                            .target(LatLng(location.latitude, location.longitude))
                            .build()
                    )
                }
                Log.d(
                    "MapViewModel",
                    "New state camera position is ${_uiState.value.cameraPosition}"
                )
            } catch (e: Exception) {
                Log.w("MapViewModel", "User location is not available", e)
                _uiState.update {
                    it.copy(inProgress = false, errorMessage = e.message)
                }
            }
        }
    }

    private fun makeBusMarker(routeNumber: Int, bus: Bus, stops: Stops): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Bus,
            bus.location.toLatLng(),
            routeNumber.toString(),
            bus.direction,
            calculateBusHeading(bus, stops)
        )

    private fun makeStopMarker(stop: Stop): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Stop,
            stop.location.toLatLng(),
            "",
            stop.direction,
            null
        )

    private fun calculateBusHeading(bus: Bus, stops: Stops): Float? {
        val nextStop = stops.items.find {
            it.id == bus.nextStopId
        }
        if (nextStop == null) {
            Log.w("MapViewModel", "Next stop with id ${bus.nextStopId} not found")
            return null
        }
        return bus.location.getHeading(nextStop.location)
    }

    private fun makeString(@StringRes resId: Int): String =
        getApplication<Application>().getString(resId)

    fun onRouteNumberChangeConfirmed(routeNumber: Int) {
        _uiState.update {
            it.copy(routNumberDialogRequired = false)
        }
        dataStore.lastRouteNumberRequestTimeInMillis = timeProvider.currentTimeMillis
        reloadRoute(routeNumber)
    }

    fun onRouteNumberChangeDismissed() {
        _uiState.update {
            it.copy(routNumberDialogRequired = false)
        }
    }

    fun onReloadRouteButtonClicked() {
        if (_uiState.value.routeNumber != null) {
            reloadRoute(_uiState.value.routeNumber!!)
        }
    }

    fun zoomToShowRoute() {
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

    private fun Location.toLatLng() = com.google.android.gms.maps.model.LatLng(lat, lon)

    private fun shouldRequestRouteNumber(): Boolean {
        if (dataStore.lastRouteNumberRequestTimeInMillis == null) {
            return true
        }
        val millisSinceLastRequest = timeProvider.currentTimeMillis -
                dataStore.lastRouteNumberRequestTimeInMillis!!
        return millisSinceLastRequest > prefs.requestRouteNumberAfterMillis
    }

    private fun requestRouteNumber() {
        _uiState.update {
            it.copy(routNumberDialogRequired = true)
        }
    }

    private fun reloadRoute(routeNumber: Int) {

        viewModelScope.launch {
            _uiState.update {
                it.copy(inProgress = true)
            }

            try {
                val route = routeProvider.getRoute(routeNumber)
                val busMarkers = route.buses.items.map {
                    makeBusMarker(routeNumber, it, route.stops)
                }
                val stopMarkers = route.stops.items.map {
                    makeStopMarker(it)
                }

                _uiState.update {
                    it.copy(
                        inProgress = false,
                        routeNumber = routeNumber,
                        routeMarkers = busMarkers + stopMarkers,
                    )
                }
            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                    )
                }
            }
        }
    }

    fun onChooseRouteButtonClicked() {
        _uiState.update {
            it.copy(routNumberDialogRequired = true)
        }
    }

    private val initialCameraPosition
        get() = run {
            if (dataStore.lastMapPosition != null) {
                dataStore.lastMapPosition!!
            } else {
                CameraPosition.Builder().target(INITIAL_LAT_LNG).zoom(INITIAL_ZOOM).build()
            }
        }
}