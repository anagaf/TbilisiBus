package com.anagaf.tbilisibus.app

import com.google.android.gms.maps.model.LatLng
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface Preferences {
    /**
     * New route number is requested if the last one was requested more than this time ago.
     */
    val routeNumberTtl: Duration

    /**
     * Route is reloaded if it's older than this duration.
     */
    val routeTtl: Duration

    /**
     * Route reload period.
     */
    val routeReloadPeriod: Duration

    /**
     * Cached route info older that this value expires.
     */
    val routeInfoCacheTtl: Duration

    /**
     * Time to wait for location.
     */
    val locationTimeout: Duration

    /**
     * City bounds.
     */
    val cityLeftTop: LatLng
    val cityRightBottom: LatLng

    /**
     * City center.
     */
    val cityCenter: LatLng
}

class PreferencesImpl : Preferences {
    override val routeNumberTtl: Duration = 15.minutes
    override val routeTtl: Duration = 1.minutes
    override val routeReloadPeriod: Duration = 15.seconds
    override val routeInfoCacheTtl: Duration = 24.hours
    override val locationTimeout: Duration = 3.seconds
    override val cityLeftTop = LatLng(41.88596, 44.5758131)
    override val cityRightBottom = LatLng(41.48988, 45.1618741)
    override val cityCenter = LatLng(41.7225, 44.7925)
}