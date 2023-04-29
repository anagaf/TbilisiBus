package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.DataProvider
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val dataProvider: DataProvider,
) : AndroidViewModel(app) {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()

    internal val busMarkers = MutableLiveData<List<BusMarkerDescription>>(emptyList())

    fun start() {
        viewModelScope.launch {
            inProgress.value = true

            try {
                val routeNumber = 306
                val buses = dataProvider.getBusesOnRoute(routeNumber)
                val stops = dataProvider.getStops(routeNumber)
                busMarkers.value = buses.items.map {
                    makeBusMarker(routeNumber, it, stops)
                }
            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                errorMessage.value = makeString(R.string.bus_locations_are_not_available)
            }

            inProgress.value = false
        }
    }

    private fun makeBusMarker(routeNumber: Int, bus: Bus, stops: Stops): BusMarkerDescription =
        BusMarkerDescription(
            bus.location,
            routeNumber.toString(),
            bus.direction,
            calculateMarkerDirection(bus, stops)
        )

    private fun calculateMarkerDirection(bus: Bus, stops: Stops): Float? {
        var closestStopLocation: Location? = null
        var closestStopDistance: Double = Double.MAX_VALUE
        stops.items.forEach { stop: Stop ->
            if (stop.direction == bus.direction) {
                val distance = bus.location.getDistance(stop.location)
                if (distance < closestStopDistance) {
                    closestStopDistance = distance
                    closestStopLocation = stop.location
                }
            }
        }
        return if (closestStopLocation == null) {
            null
        } else {
            bus.location.getDirection(closestStopLocation!!)
        }

    }

    private fun makeString(@StringRes resId: Int): String =
        getApplication<Application>().getString(resId)
}