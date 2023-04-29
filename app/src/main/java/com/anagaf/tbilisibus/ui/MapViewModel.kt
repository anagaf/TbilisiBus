package com.anagaf.tbilisibus.ui

import android.app.Application
import android.graphics.Color
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.DataProvider
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val busLocationProvider: DataProvider,
) : AndroidViewModel(app) {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()

    internal val busMarkers = MutableLiveData<List<BusMarkerDescription>>(emptyList())

    fun start() {
        Log.i("MapViewModel", "start map view model")
        viewModelScope.launch {
            inProgress.value = true

            try {
                val newBusMarkers = mutableListOf<BusMarkerDescription>()
                val routeNumber = 306
                val buses306 = busLocationProvider.getBusesOnRoute(routeNumber)
                newBusMarkers += buses306.forward.locations.map {
                    BusMarkerDescription(
                        it,
                        routeNumber.toString(),
                        BitmapDescriptorFactory.HUE_RED,
                        0f
                    )
                }
                newBusMarkers += buses306.backward.locations.map {
                    BusMarkerDescription(
                        it,
                        routeNumber.toString(),
                        BitmapDescriptorFactory.HUE_BLUE,
                        0f
                    )
                }
                busMarkers.value = newBusMarkers
            } catch (ex: Exception) {
                Log.e("MapViewModel", "Cannot retrieve bus locations: ${ex.message}")
                errorMessage.value = makeString(R.string.bus_locations_are_not_available)
            }

            inProgress.value = false
        }
    }

    private fun makeString(@StringRes resId: Int): String =
        getApplication<Application>().getString(resId)
}