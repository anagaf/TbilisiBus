package com.anagaf.tbilisibus.ui

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val busLocationProvider: BusLocationsProvider,
) : ViewModel() {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()

    internal val busLocations = MutableLiveData<List<BusLocation>>(emptyList());

    fun start() {
        Log.i("MapViewModel", "start map view model")
        viewModelScope.launch {
            inProgress.value = true

            val bus306Locations = busLocationProvider.getBusLocations(306)
            Log.i("MapViewModel", "${bus306Locations.locations.size} locations found for bus #306:")
            bus306Locations.locations.forEach() {
                Log.i("MapViewModel", "-- ${it.coords.lat}, ${it.coords.lon}")
            }

            inProgress.value = false
        }
    }
}