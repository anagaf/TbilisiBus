package com.anagaf.tbilisibus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.RouteRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val dataStore: AppDataStore,
    private val prefs: Preferences,
    private val timeProvider: TimeProvider,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    companion object {
        val kInitialCameraPosition = CameraPosition.Builder()
            .target(LatLng(41.7225, 44.7925)) // Tbilisi center
            .zoom(12f)
            .build()
        val kMyLocationZoom = 15f
    }

    private val _uiState = MutableStateFlow(
        value = MapUiState(
            cameraPosition = kInitialCameraPosition
        )
    )

    val uiState = _uiState.asStateFlow()

    var routeUpdateJob: Job? = null

    fun onMapReady() {
        if (dataStore.lastCameraPosition != null) {
            _uiState.update { it.copy(cameraPosition = dataStore.lastCameraPosition!!) }
        }

        if (_uiState.value.route == null || shouldRequestRouteNumber()) {
            requestRouteNumber()
        } else if (shouldReloadRoute()) {
            retrieveRoute(_uiState.value.route!!.number)
        }
    }

    fun onCameraMove(pos: CameraPosition) {
        dataStore.lastCameraPosition = pos
        _uiState.update {
            it.copy(cameraPosition = pos, cameraBounds = null)
        }
    }

    fun onMyLocationButtonClicked() {
        moveCameraToCurrentLocation()
    }

    fun onActivityStart() {
        startPeriodicRouteUpdate()
    }

    fun onActivityStop() {
        stopPeriodicRouteUpdate()
    }

    private fun moveCameraToCurrentLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(inProgress = true)
            }
            try {
                val location = locationProvider.getLastLocation()
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        cameraPosition = CameraPosition.Builder(it.cameraPosition)
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(kMyLocationZoom)
                            .build()
                    )
                }
            } catch (ex: Exception) {
                Timber.e("User location is not available: ${ex.message}")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        error = MapUiState.Error.LocationNotAvailable,
                    )
                }
            }
        }
    }

    fun onRouteNumberChosen(routeNumber: Int) {
        _uiState.update {
            it.copy(dialogRequired = null)
        }
        retrieveRoute(routeNumber)
    }

    fun onRouteNumberChangeDismissed() {
        _uiState.update {
            it.copy(dialogRequired = null)
        }
    }

    fun onReloadRouteButtonClicked() {
        if (_uiState.value.route != null) {
            retrieveRoute(_uiState.value.route!!.number)
        }
    }

    fun onZoomToShowRouteButtonClicked() {
        _uiState.value.route?.let {
            _uiState.update {
                it.copy(cameraBounds = it.route!!.bounds)
            }
        }
    }

    private fun shouldRequestRouteNumber(): Boolean =
        dataStore.lastRouteNumberRequestTime?.let { time ->
            timeProvider.now - time > prefs.routeNumberTtl
        } ?: true

    private fun shouldReloadRoute(): Boolean =
        dataStore.lastRouteNumberRequestTime?.let { time ->
            timeProvider.now - time > prefs.routeTtl
        } ?: true

    private fun requestRouteNumber() {
        _uiState.update {
            it.copy(dialogRequired = MapUiState.Dialog.Route)
        }
    }

    private fun retrieveRoute(routeNumber: Int) {

        viewModelScope.launch {
            _uiState.update {
                it.copy(inProgress = true)
            }

            try {
                val route = routeRepository.getRoute(routeNumber)
                dataStore.lastRouteNumberRequestTime = timeProvider.now

                var bounds = route.bounds
                getLocationIfAvailable()?.let { location ->
                    bounds = bounds.including(location)
                }

                _uiState.update {
                    it.copy(
                        inProgress = false,
                        route = route,
                        cameraBounds = bounds
                    )
                }

                startPeriodicRouteUpdate()
            } catch (ex: Exception) {
                Timber.e(ex, "Route request failed")
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        error = MapUiState.Error.RouteNotAvailable
                    )
                }
            }
        }
    }

    private fun startPeriodicRouteUpdate() {
        stopPeriodicRouteUpdate()
        routeUpdateJob = viewModelScope.launch {
            while (true) {
                delay(prefs.routeReloadPeriod)
                doRouteUpdateIfPossible()
            }
        }
    }

    private suspend fun doRouteUpdateIfPossible() =
        uiState.value.route?.number?.let { routeNumber ->
            if (uiState.value.inProgress) {
                return@let
            }

            Timber.d("Periodic route $routeNumber update")

            try {
                val route = routeRepository.getRoute(routeNumber)
                dataStore.lastRouteNumberRequestTime = timeProvider.now

                _uiState.update {
                    it.copy(
                        route = route,
                    )
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Route request failed")
            }
        }

    private fun stopPeriodicRouteUpdate() {
        routeUpdateJob?.apply {
            if (isActive) {
                cancel()
            }
        }
        routeUpdateJob = null
    }

    fun onChooseRouteButtonClicked() {
        requestRouteNumber()
    }

    fun onErrorMessageShown() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    private suspend fun getLocationIfAvailable(): LatLng? = try {
        locationProvider.getLastLocation()
    } catch (ex: Exception) {
        Timber.d("User location is not available: ${ex.message}")
        null
    }

    fun onAboutClicked() {
        _uiState.update {
            it.copy(dialogRequired = MapUiState.Dialog.About)
        }
    }

    fun onAboutDialogDismissed() {
        _uiState.update {
            it.copy(dialogRequired = null)
        }
    }
}