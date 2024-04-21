package com.anagaf.tbilisibus.app

import android.content.Context
import android.content.SharedPreferences
import com.anagaf.tbilisibus.ui.UiAlignment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.datetime.Instant
import javax.inject.Inject

interface AppDataStore {
    var lastCameraPosition: CameraPosition?
    var lastRouteNumberRequestTime: Instant?
    var uiAlignment: UiAlignment
}

class AppDataStoreImpl @Inject constructor(context: Context) : AppDataStore {

    private val prefs: SharedPreferences

    companion object {
        const val LatKey = "lat"
        const val LonKey = "lon"
        const val ZoomKey = "zoom"
        const val LastRouteNumberRequestEpochSecondsKey = "lastRouteNumberRequestTimeInMillis"
        const val UiAlignmentKey = "uiAlignment"
        const val UiAlignmentLeft = 1
        const val UiAlignmentRight = 2
    }

    init {
        prefs = context.getSharedPreferences("general", Context.MODE_PRIVATE)
    }

    private fun getFloat(key: String): Float? =
        if (prefs.contains(key)) prefs.getFloat(key, 0f) else null

    override var lastCameraPosition: CameraPosition?
        get() {
            val lat = getFloat(LatKey)
            val lon = getFloat(LonKey)
            val zoom = getFloat(ZoomKey)
            return if (lat != null && lon != null && zoom != null)
                CameraPosition.fromLatLngZoom(LatLng(lat.toDouble(), lon.toDouble()), zoom)
            else null
        }
        set(value) {
            assert(value != null)
            prefs.edit().putFloat(LatKey, value!!.target.latitude.toFloat())
                .putFloat(LonKey, value.target.longitude.toFloat())
                .putFloat(ZoomKey, value.zoom).apply()
        }

    override var lastRouteNumberRequestTime: Instant?
        get() = if (prefs.contains(LastRouteNumberRequestEpochSecondsKey)) {
            val epochSeconds = prefs.getLong(LastRouteNumberRequestEpochSecondsKey, 0L)
            Instant.fromEpochSeconds(epochSeconds)
        } else {
            null
        }
        set(value) {
            assert(value != null)
            prefs.edit().putLong(LastRouteNumberRequestEpochSecondsKey, value!!.epochSeconds)
                .apply()
        }

    override var uiAlignment: UiAlignment
        get() =
            when (prefs.getInt(UiAlignmentKey, UiAlignmentRight)) {
                UiAlignmentLeft -> UiAlignment.Left
                UiAlignmentRight -> UiAlignment.Right
                else -> UiAlignment.Right
            }
        set(value) =
            prefs.edit().putInt(
                UiAlignmentKey,
                when (value) {
                    UiAlignment.Left -> UiAlignmentLeft
                    UiAlignment.Right -> UiAlignmentRight
                }
            ).apply()
}