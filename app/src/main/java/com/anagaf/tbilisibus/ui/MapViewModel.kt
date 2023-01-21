package com.anagaf.tbilisibus.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

@HiltViewModel
class MapViewModel @Inject constructor(
    val busLocationProvider: BusLocationProvider,
) : ViewModel() {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()

    internal val busLocations = MutableLiveData<List<BusLocation>>(emptyList());

    fun start() {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {
        inProgress.value = true

        val bus306Locations = busLocationProvider.getBusLocations(306)
        logger.debug { "${bus306Locations.size} locations found for bus #306:" }
        bus306Locations.forEach {
            logger.debug { "-- ${it.coords.lat}, ${it.coords.lon}" }
        }

        inProgress.value = false
    }
}