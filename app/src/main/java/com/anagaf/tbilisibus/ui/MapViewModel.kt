package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
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
    val state = MutableLiveData<MapUiState>()

    fun onMapReady() {
        viewModelScope.launch {
            if (dataStore.lastMapPosition != null) {
                state.value = MapUiState.CameraMoveRequired(
                    dataStore.lastMapPosition!!, state.value?.route
                )
            }
        }
        if (state.value?.route == null) {
            requestRouteNumber()
        } else {
            if (shouldRequestRouteNumber()) {
                requestRouteNumber()
            } else {
                onRouteNumberChanged(state.value!!.route!!.routeNumber)
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
        assert(state.value?.route != null)
        reloadRoute(state.value!!.route!!.routeNumber)
    }

    fun zoomToShowRoute() {
        assert(state.value?.route != null)
        val markerBounds = LatLngBounds.builder().apply {
            state.value!!.route!!.markers.forEach { marker: MapUiState.Marker ->
                include(marker.location.toLatLng())
            }
        }.build()
        state.value = MapUiState.CameraShowBoundsRequired(markerBounds, state.value?.route)
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
        state.value = MapUiState.RouteNumberRequired(state.value?.route)
    }

    private fun reloadRoute(routeNumber: Int) {

        viewModelScope.launch {
            state.value = MapUiState.InProgress(state.value?.route)

            try {
                val route = routeProvider.getRoute(routeNumber)
                val busMarkers = route.buses.items.map {
                    makeBusMarker(routeNumber, it, route.stops)
                }
                val stopMarkers = route.stops.items.map {
                    makeStopMarker(it)
                }

                state.value = MapUiState.RouteAvailable(
                    MapUiState.RouteUiState(
                        routeNumber,
                        busMarkers + stopMarkers
                    )
                )

            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                state.value = MapUiState.Error(
                    makeString(R.string.bus_locations_are_not_available),
                    state.value?.route
                )
            }
        }
    }
}