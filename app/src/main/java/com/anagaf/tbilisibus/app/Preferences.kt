package com.anagaf.tbilisibus.app

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface Preferences {
    /**
     * New route number is requested if the last one was requested more than this time ago.
     */
    val routeNumberTtl: Duration
        get() = 15.minutes

    /**
     * Route is reloaded if it's older than this duration.
     */
    val routeTtl: Duration
        get() = 1.minutes

    /**
     * Route reload period.
     */
    val routeReloadPeriod: Duration
        get() = 15.seconds

    /**
     * Cached route info older that this value expires.
     */
    val routeInfoCacheTtl: Duration
        get() = 24.hours

    /**
     * Time to wait for location.
     */
    val locationTimeout: Duration
        get() = 3.seconds

    /**
     * City bounds.
     */
    val cityBounds: LatLngBounds
        get() = LatLngBounds(
            // SW
            LatLng(41.48988, 44.5758131),
            // NE
            LatLng(41.88596, 45.1618741)
        )
}
