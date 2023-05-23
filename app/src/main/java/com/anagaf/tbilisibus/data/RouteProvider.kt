package com.anagaf.tbilisibus.data

interface RouteProvider {
    suspend fun getRoute(routeNumber: Int): Route
}