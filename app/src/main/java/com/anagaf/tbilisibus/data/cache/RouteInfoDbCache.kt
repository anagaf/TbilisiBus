package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.RouteInfo
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.ui.TimeProvider
import com.google.android.gms.maps.model.LatLng
import kotlinx.datetime.Instant
import kotlin.time.Duration

class RouteInfoCacheImpl(
    private val routeInfoDao: RouteInfoDao,
    private val timeProvider: TimeProvider,
    private val routeInfoTtl: Duration
) : RouteInfoCache {

    override fun getRouteInfo(routeNumber: Int): RouteInfo? {
        val routeInfo = routeInfoDao.get(routeNumber)
        if (routeInfo == null || isRouteInfoExpired(routeInfo.routeInfo.timestamp)) {
            return null;
        }

        val stops = routeInfo.stops.groupBy {
            it.direction
        }.mapValues {
            it.value.map { stop ->
                Stop(position = LatLng(stop.latitude, stop.longitude))
            }
        }

        val shapePoints = routeInfo.shapePoints.groupBy {
            it.direction
        }.mapValues {
            it.value.map { point ->
                ShapePoint(position = LatLng(point.latitude, point.longitude))
            }
        }

        return RouteInfo(stops, shapePoints)
    }

    override fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo) {
        // do nothing
    }

    private fun isRouteInfoExpired(timestamp: Instant): Boolean =
        timestamp < timeProvider.now - routeInfoTtl
}