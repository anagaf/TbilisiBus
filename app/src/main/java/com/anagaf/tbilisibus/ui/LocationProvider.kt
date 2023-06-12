package com.anagaf.tbilisibus.ui

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.suspendCoroutine

interface LocationProvider {
    suspend fun getLastLocation(): LatLng
}

class SystemLocationProvider(context:Context) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): LatLng {
        return suspendCoroutine {
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    it.resumeWith(Result.success(LatLng(location.latitude, location.longitude)))
                } else {
                    it.resumeWith(Result.failure(Exception("Location is not available")))
                }
            }
        }
    }
}