package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val busLocationProvider: BusLocationsProvider,
) : AndroidViewModel(app) {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()

    internal val busLocations = MutableLiveData<List<BusLocation>>(emptyList())

    fun start() {
        Log.i("MapViewModel", "start map view model")
        viewModelScope.launch {
            inProgress.value = true

            try {
                val bus306Locations = busLocationProvider.getBusLocations(306)
                Log.i("MapViewModel", "${bus306Locations.size} locations found for bus #306:")
                bus306Locations.forEach() {
                    Log.i("MapViewModel", "-- ${it.coords.lat}, ${it.coords.lon}")
                }
                busLocations.value = bus306Locations
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