package com.anagaf.tbilisibus.data

interface BusLocationProvider {
    suspend fun getBusLocations(routeNumber: Int): List<BusLocation>
}