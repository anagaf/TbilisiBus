package com.anagaf.tbilisibus.ui

interface TimeProvider {
    val currentTimeMillis: Long
}

class SystemTimeProvider : TimeProvider {
    override val currentTimeMillis: Long
        get() = System.currentTimeMillis()
}