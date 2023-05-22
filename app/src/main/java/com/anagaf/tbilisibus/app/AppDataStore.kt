package com.anagaf.tbilisibus.app

import android.content.Context
import android.content.SharedPreferences
import com.anagaf.tbilisibus.ui.CameraPosition
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

interface AppDataStore {
    var lastMapPosition: CameraPosition?
    var lastRouteNumberRequestTimeInMillis: Long?
}

class AppDataStoreImpl @Inject constructor(context: Context) : AppDataStore {

    private val prefs: SharedPreferences

    companion object {
        const val LatKey = "lat"
        const val LonKey = "lon"
        const val ZoomKey = "zoom"
        const val LastRouteNumberRequestTimeInMillisKey = "lastRouteNumberRequestTimeInMillis"
    }

    init {
        prefs = context.getSharedPreferences("general", Context.MODE_PRIVATE)
    }

    private fun getFloat(key: String): Float? =
        if (prefs.contains(key)) prefs.getFloat(key, 0f) else null

    override var lastMapPosition: CameraPosition?
        get() {
            val lat = getFloat(LatKey)
            val lon = getFloat(LonKey)
            val zoom = getFloat(ZoomKey)
            return if (lat != null && lon != null && zoom != null)
                CameraPosition(LatLng(lat.toDouble(), lon.toDouble()), zoom)
            else null
        }
        set(value) {
            assert(value != null)
            prefs.edit().putFloat(LatKey, value!!.latLng.latitude.toFloat())
                .putFloat(LonKey, value.latLng.longitude.toFloat())
                .putFloat(ZoomKey, value.zoom).apply()
        }

    override var lastRouteNumberRequestTimeInMillis: Long?
        get() = if (prefs.contains(LastRouteNumberRequestTimeInMillisKey))
            prefs.getLong(LastRouteNumberRequestTimeInMillisKey, 0L)
        else null
        set(value) {
            assert(value != null)
            prefs.edit().putLong(LastRouteNumberRequestTimeInMillisKey, value!!).apply()
        }
}