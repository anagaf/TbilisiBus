package com.anagaf.tbilisibus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RouteRepository {
    suspend fun getRoute(routeNumber: Int): Route
}

class RouteRepositoryImpl(
    private val busesDataSource: BusesDataSource,
    private val routeInfoDataSource: RouteInfoDataSource,
    private val routeInfoCache: RouteInfoCache
) : RouteRepository {

    override suspend fun getRoute(routeNumber: Int): Route =
        withContext(Dispatchers.IO) {
            val buses = busesDataSource.getBuses(routeNumber)
            val routeInfo = getRouteInfo(routeNumber)
            Route(
                routeNumber,
                makeElement(buses, routeInfo, Direction.Forward),
                makeElement(buses, routeInfo, Direction.Backward)
            )
        }

    private fun makeElement(buses: BusesByDirection, routeInfo: RouteInfo, direction: Direction) =
        Route.Elements(
            buses[direction]!!,
            routeInfo.stops[direction]!!,
            routeInfo.shapePoints[direction]!!
        )


    private suspend fun getRouteInfo(routeNumber: Int): RouteInfo {
        var routeInfo = routeInfoCache.getRouteInfo(routeNumber)
        if (routeInfo == null) {
            routeInfo = routeInfoDataSource.getRouteInfo(routeNumber)
            routeInfoCache.setRouteInfo(routeNumber, routeInfo)
        }
        return routeInfo
    }
}