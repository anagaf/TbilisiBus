package com.anagaf.tbilisibus.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    val busLocationProvider: BusLocationProvider,
) : ViewModel() {

    val inProgress: MutableLiveData<Boolean> = MutableLiveData()

    var busLocations: List<BusLocation> = emptyList()

    fun start() {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {
        inProgress.value = true
        busLocations = busLocationProvider.getBusLocations()
        inProgress.value = false
    }
}