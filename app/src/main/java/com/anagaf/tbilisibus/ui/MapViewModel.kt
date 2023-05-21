package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.Preferences
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.SituationProvider
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val situationProvider: SituationProvider,
    private val preferences: Preferences
) : AndroidViewModel(app) {
    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()

    class CameraParams {
        private constructor(position: MapCameraPosition?, bounds: LatLngBounds?) {
            this.position = position
            this.bounds = bounds
        }

        constructor(position: MapCameraPosition) : this(position, null)
        constructor(bounds: LatLngBounds) : this(null, bounds)

        val position: MapCameraPosition?
        val bounds: LatLngBounds?
    }

    val cameraParams = MutableLiveData<CameraParams>()

    internal val situationImage = MutableLiveData<SituationImage?>()

    fun start() {
        viewModelScope.launch {
            inProgress.value = false
            if (preferences.lastMapPosition != null) {
                cameraParams.value = CameraParams(preferences.lastMapPosition!!)
            }
        }
    }

    fun onCameraMove(pos: MapCameraPosition) {
        preferences.lastMapPosition = pos
    }

    private fun makeBusMarker(routeNumber: Int, bus: Bus, stops: Stops): SituationImage.Marker =
        SituationImage.Marker(
            SituationImage.Marker.Type.Bus,
            bus.location,
            routeNumber.toString(),
            bus.direction,
            calculateBusHeading(bus, stops)
        )

    private fun makeStopMarker(stop: Stop): SituationImage.Marker =
        SituationImage.Marker(
            SituationImage.Marker.Type.Stop,
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

    fun updateSituation(routeNumber: Int) {
        viewModelScope.launch {
            situationImage.value = null
            inProgress.value = true

            try {
                val situation = situationProvider.getSituation(routeNumber)
                val busMarkers = situation.buses.items.map {
                    makeBusMarker(routeNumber, it, situation.stops)
                }
                val stopMarkers = situation.stops.items.map {
                    makeStopMarker(it)
                }
                situationImage.value =
                    SituationImage(situation.routeNumber, busMarkers + stopMarkers)

            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                errorMessage.value = makeString(R.string.bus_locations_are_not_available)
            }

            inProgress.value = false
        }
    }

    fun updateSituation() {
        assert(situationImage.value != null)
        updateSituation(situationImage.value!!.routeNumber)
    }

    fun zoomToShowRoute() {
        val markerBounds = LatLngBounds.builder().apply {
            situationImage.value?.markers?.forEach { marker: SituationImage.Marker ->
                include(marker.location.toLatLng())
            }
        }.build()
        cameraParams.value = CameraParams(markerBounds)
    }

    private fun Location.toLatLng() = com.google.android.gms.maps.model.LatLng(lat, lon)

}