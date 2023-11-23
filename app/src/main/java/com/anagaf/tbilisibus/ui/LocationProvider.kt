package com.anagaf.tbilisibus.ui

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await

interface LocationProvider {
    suspend fun getLastLocation(): LatLng
}

class SystemLocationProvider(context: Context) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): LatLng = client.lastLocation.await().let {
        LatLng(it.latitude, it.longitude)
    }
}