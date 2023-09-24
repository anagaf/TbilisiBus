package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.RouteInfo
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.ui.TimeProvider
import com.google.android.gms.maps.model.LatLng
import kotlinx.datetime.Instant
import timber.log.Timber
import kotlin.time.Duration

class RouteInfoCacheImpl(
    private val routeInfoDao: RouteInfoDao,
    private val timeProvider: TimeProvider,
    private val routeInfoTtl: Duration
) : RouteInfoCache {

    override suspend fun getRouteInfo(routeNumber: Int): RouteInfo? {
        val routeInfo = routeInfoDao.get(routeNumber)
        if (routeInfo == null) {
            Timber.d("Cached route $routeNumber info not found")
            return null
        }
        if (isRouteInfoExpired(routeInfo.routeInfo.timestamp)) {
            Timber.d("Cached route $routeNumber info expired")
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

    override suspend fun setRouteInfo(routeNumber: Int, routeInfo: RouteInfo) {
        val routeInfoEntity =
            RouteInfoEntity(routeNumber = routeNumber, timestamp = timeProvider.now)
        val stopEntities = routeInfo.stops.entries.flatMap {
            it.value.map { stop ->
                StopEntity(
                    routeNumber = routeNumber,
                    direction = it.key,
                    latitude = stop.position.latitude,
                    longitude = stop.position.longitude
                )
            }
        }
        val shapePointEntities = routeInfo.shapePoints.entries.flatMap {
            it.value.map { stop ->
                ShapePointEntity(
                    routeNumber = routeNumber,
                    direction = it.key,
                    latitude = stop.position.latitude,
                    longitude = stop.position.longitude
                )
            }
        }
        routeInfoDao.insert(
            RouteInfoWithStopsAndShapePoints(
                routeInfoEntity,
                stopEntities,
                shapePointEntities
            )
        )

        Timber.d("Updated cache route $routeNumber info (${stopEntities.size} stops, ${shapePointEntities.size} shape points)")
    }

    private fun isRouteInfoExpired(timestamp: Instant): Boolean =
        timestamp < timeProvider.now - routeInfoTtl
}