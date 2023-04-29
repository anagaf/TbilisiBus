package com.anagaf.tbilisibus.data

interface BusesOnRouteProvider {
    suspend fun getBusesOnRoute(routeNumber: Int): BusesOnRoute
}