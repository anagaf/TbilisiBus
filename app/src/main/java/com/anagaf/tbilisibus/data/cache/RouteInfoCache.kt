package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.RouteInfo

interface RouteInfoCache {
    fun getRouteInfo(routeNumber: Int): RouteInfo?
    fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo)
}

