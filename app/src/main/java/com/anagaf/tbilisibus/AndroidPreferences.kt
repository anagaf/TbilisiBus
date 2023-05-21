package com.anagaf.tbilisibus

import android.content.Context
import android.content.SharedPreferences
import com.anagaf.tbilisibus.ui.CameraPosition
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class AndroidPreferences @Inject constructor(context: Context) : Preferences {

    private val prefs: SharedPreferences

    companion object {
        const val LatKey = "lat"
        const val LonKey = "lon"
        const val ZoomKey = "zoom"
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
            if (value != null) {
                prefs.edit().putFloat(LatKey, value.latLng.latitude.toFloat())
                    .putFloat(LonKey, value.latLng.longitude.toFloat())
                    .putFloat(ZoomKey, value.zoom).apply()

            }
        }
}