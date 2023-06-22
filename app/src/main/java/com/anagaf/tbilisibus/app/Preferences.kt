package com.anagaf.tbilisibus.app

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface Preferences {
    /**
     * New route number is requested if the last one was requested more than this time ago.
     */
    val routeNumberTtl: Duration
}

class PreferencesImpl : Preferences {
    override val routeNumberTtl: Duration = 5.minutes
}