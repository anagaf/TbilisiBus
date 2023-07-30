package com.anagaf.tbilisibus.app

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface Preferences {
    /**
     * New route number is requested if the last one was requested more than this time ago.
     */
    val routeNumberTtl: Duration

    /**
     * Route is reloaded if it's older than this duration.
     */
    val routeTtl: Duration
}

class PreferencesImpl : Preferences {
    override val routeNumberTtl: Duration = 15.minutes
    override val routeTtl: Duration = 1.minutes
}