package com.anagaf.tbilisibus.data

interface DataProvider {
    suspend fun getBusesOnRoute(routeNumber: Int): BusesOnRoute
}