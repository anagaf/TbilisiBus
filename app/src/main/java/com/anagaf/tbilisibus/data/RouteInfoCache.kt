package com.anagaf.tbilisibus.data

interface RouteInfoCache {
    fun getRouteInfo(routeNumber: Int): RouteInfo?
    fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo)
}

class RouteInfoCacheImpl: RouteInfoCache {
    override fun getRouteInfo(routeNumber: Int): RouteInfo? {
        return null
    }

    override fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo) {
        // do nothing
    }

}