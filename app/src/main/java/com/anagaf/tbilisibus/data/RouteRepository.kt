package com.anagaf.tbilisibus.data

import com.anagaf.tbilisibus.data.cache.RouteInfoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
        val cachedRouteInfo = routeInfoCache.getRouteInfo(routeNumber)
        if (cachedRouteInfo != null) {
            Timber.d("Read route $routeNumber info from cache")
            return cachedRouteInfo
        }
        val removeRouteInfo = routeInfoDataSource.getRouteInfo(routeNumber)
        Timber.d("Retrieved route $routeNumber info from remote data source")
        routeInfoCache.setRouteInfo(routeNumber, removeRouteInfo)
        return removeRouteInfo
    }
}