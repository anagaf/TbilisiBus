package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.RouteInfo

class RouteInfoCacheImpl: RouteInfoCache {
    override fun getRouteInfo(routeNumber: Int): RouteInfo? {
        return null
    }

    override fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo) {
        // do nothing
    }

}