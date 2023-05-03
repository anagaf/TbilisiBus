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
import com.anagaf.tbilisibus.data.DataProvider
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val dataProvider: DataProvider,
    private val preferences: Preferences
) : AndroidViewModel(app) {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()
    val initialCameraPos = MutableLiveData<MapCameraPosition>()

    internal val markers = MutableLiveData<List<MarkerDescription>>(emptyList())

    fun start() {
        viewModelScope.launch {
            if (preferences.lastMapPosition != null) {
                initialCameraPos.value = preferences.lastMapPosition!!
            }

            inProgress.value = true

            try {
                val routeNumber = 306
                val buses = dataProvider.getBusesOnRoute(routeNumber)
                val stops = dataProvider.getStops(routeNumber)
                val busMarkers = buses.items.map {
                    makeBusMarker(routeNumber, it, stops)
                }
                val stopMarkers = stops.items.map {
                    makeStopMarker(it)
                }
                markers.value = busMarkers + stopMarkers
            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                errorMessage.value = makeString(R.string.bus_locations_are_not_available)
            }

            inProgress.value = false
        }
    }

    fun onCameraMove(pos: MapCameraPosition) {
        preferences.lastMapPosition = pos
    }

    private fun makeBusMarker(routeNumber: Int, bus: Bus, stops: Stops): MarkerDescription =
        MarkerDescription(
            MarkerType.Bus,
            bus.location,
            routeNumber.toString(),
            bus.direction,
            calculateBusHeading(bus, stops)
        )

    private fun makeStopMarker(stop: Stop): MarkerDescription =
        MarkerDescription(
            MarkerType.Stop,
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
        return bus.location.getHeading(nextStop!!.location)
    }

    private fun makeString(@StringRes resId: Int): String =
        getApplication<Application>().getString(resId)
}