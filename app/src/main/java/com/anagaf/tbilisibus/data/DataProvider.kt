package com.anagaf.tbilisibus.data

interface DataProvider {
    suspend fun getBusesOnRoute(routeNumber: Int): Buses
    suspend fun getStops(routeNumber: Int): Stops
}