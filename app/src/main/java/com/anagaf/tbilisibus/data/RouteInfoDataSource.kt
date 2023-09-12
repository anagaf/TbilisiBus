package com.anagaf.tbilisibus.data

interface RouteInfoDataSource {
    suspend fun getRouteInfo(routeNumber: Int): RouteInfo
}