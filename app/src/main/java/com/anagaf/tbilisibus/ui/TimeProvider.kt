package com.anagaf.tbilisibus.ui

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface TimeProvider {
    val now: Instant
}

class SystemTimeProvider : TimeProvider {
    override val now: Instant
        get() = Clock.System.now()
}