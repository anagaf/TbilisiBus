package com.anagaf.tbilisibus.data

interface BusLocationProvider {
    suspend fun getBusLocations(): List<BusLocation>
}