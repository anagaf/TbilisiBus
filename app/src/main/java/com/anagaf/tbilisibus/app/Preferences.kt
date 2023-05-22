package com.anagaf.tbilisibus.app

interface Preferences {
    val requestRouteNumberAfterMillis: Long
}

class PreferencesImpl : Preferences {
    override val requestRouteNumberAfterMillis: Long = 5 * 60 * 1000
}