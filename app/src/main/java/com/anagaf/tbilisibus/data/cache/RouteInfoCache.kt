package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.RouteInfo

interface RouteInfoCache {
    suspend fun getRouteInfo(routeNumber: Int): RouteInfo?
    suspend fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo)
}

