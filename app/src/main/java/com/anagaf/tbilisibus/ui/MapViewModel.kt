package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val routeProvider: RouteProvider,
    private val dataStore: AppDataStore,
    private val prefs: Preferences,
    private val timeProvider: TimeProvider
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<MapUiState>(value = MapUiState.Initial())
    val uiState = _uiState.asStateFlow()

    fun onMapReady() {
        viewModelScope.launch {
            if (dataStore.lastMapPosition != null) {
                _uiState.value = MapUiState.CameraMoveRequired(
                    dataStore.lastMapPosition!!, _uiState.value
                )
            }
        }
        if (_uiState.value.route == null) {
            requestRouteNumber()
        } else {
            if (shouldRequestRouteNumber()) {
                requestRouteNumber()
            } else {
                onRouteNumberChanged(_uiState.value.route!!.number)
            }
        }
    }

    fun onCameraMove(pos: CameraPosition) {
        dataStore.lastMapPosition = pos
    }

    private fun makeBusMarker(routeNumber: Int, bus: Bus, stops: Stops): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Bus,
            bus.location,
            routeNumber.toString(),
            bus.direction,
            calculateBusHeading(bus, stops)
        )

    private fun makeStopMarker(stop: Stop): MapUiState.Marker =
        MapUiState.Marker(
            MapUiState.Marker.Type.Stop,
            stop.location,
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

    fun onRouteNumberChanged(routeNumber: Int) {
        dataStore.lastRouteNumberRequestTimeInMillis = timeProvider.currentTimeMillis
        reloadRoute(routeNumber)
    }

    fun reloadCurrentRoute() {
        assert(_uiState.value.route != null)
        reloadRoute(_uiState.value.route!!.number)
    }

    fun zoomToShowRoute() {
        assert(_uiState.value.route != null)
        val markerBounds = LatLngBounds.builder().apply {
            _uiState.value.route!!.markers.forEach { marker: MapUiState.Marker ->
                include(marker.location.toLatLng())
            }
        }.build()
        _uiState.value = MapUiState.CameraShowBoundsRequired(markerBounds, _uiState.value)
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
        _uiState.value = MapUiState.RouteNumberRequired(_uiState.value)
    }

    private fun reloadRoute(routeNumber: Int) {

        viewModelScope.launch {
            _uiState.value = MapUiState.InProgress(_uiState.value)

            try {
                val route = routeProvider.getRoute(routeNumber)
                val busMarkers = route.buses.items.map {
                    makeBusMarker(routeNumber, it, route.stops)
                }
                val stopMarkers = route.stops.items.map {
                    makeStopMarker(it)
                }

                _uiState.value = MapUiState.Idle(
                    MapUiState.RouteUiState(
                        routeNumber,
                        busMarkers + stopMarkers
                    )
                )

            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                _uiState.value = MapUiState.Error(
                    _uiState.value,
                    makeString(R.string.bus_locations_are_not_available),
                )
            }
        }
    }
}