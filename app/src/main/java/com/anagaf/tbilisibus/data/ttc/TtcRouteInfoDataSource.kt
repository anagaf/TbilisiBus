package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.RouteInfo
import com.anagaf.tbilisibus.data.RouteInfoDataSource
import javax.inject.Inject

class TtcRouteInfoDataSource @Inject constructor(private val retrofitService: TtcRetrofitService) :
    RouteInfoDataSource {

    override suspend fun getRouteInfo(routeNumber: Int): RouteInfo {
        val forward = requestRouteInfo(routeNumber, Direction.Forward)
        val backward = requestRouteInfo(routeNumber, Direction.Backward)
        val stops = mapOf(Direction.Forward to forward.stops, Direction.Backward to backward.stops)
        val shapePoints = mapOf(
            Direction.Forward to forward.shapePoints,
            Direction.Backward to backward.shapePoints
        )
        return RouteInfo(stops, shapePoints)
    }

    private fun requestRouteInfo(routeNumber: Int, direction: Direction): DirectionRouteInfo {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getRouteInfo(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Route shape request failed with code ${response.code()}")
        }
        if (response.body() == null) {
            throw RuntimeException("Route shape response is empty")
        }
        return response.body()!!
    }
}