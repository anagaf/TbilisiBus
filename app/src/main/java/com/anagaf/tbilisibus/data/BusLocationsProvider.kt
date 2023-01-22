package com.anagaf.tbilisibus.data

interface BusLocationsProvider {
    suspend fun getBusLocations(routeNumber: Int): BusLocations
}