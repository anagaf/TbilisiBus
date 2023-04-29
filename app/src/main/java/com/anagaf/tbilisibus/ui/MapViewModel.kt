package com.anagaf.tbilisibus.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.BusOnRoute
import com.anagaf.tbilisibus.data.BusesOnRouteProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val busLocationProvider: BusesOnRouteProvider,
) : AndroidViewModel(app) {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage = MutableLiveData<String>()

    internal val busLocations = MutableLiveData<List<BusOnRoute>>(emptyList())

    fun start() {
        Log.i("MapViewModel", "start map view model")
        viewModelScope.launch {
            inProgress.value = true

            try {
                val bus306 = busLocationProvider.getBusesOnRoute(306)
                busLocations.value = bus306.buses
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