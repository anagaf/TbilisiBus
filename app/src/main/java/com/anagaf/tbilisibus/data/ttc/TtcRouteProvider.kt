package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.RouteProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TtcRouteProvider @Inject constructor(private val retrofitService: TtcRetrofitService) :
    RouteProvider {

    override suspend fun getRoute(routeNumber: Int): Route =
        withContext(Dispatchers.IO) {
            Route(
                routeNumber,
                requestElements(routeNumber, Direction.Forward),
                requestElements(routeNumber, Direction.Backward)
            )
        }

    private fun requestElements(routeNumber: Int, direction: Direction): Route.Elements {
        val buses = requestBuses(routeNumber, direction)
        val routeInfo = requestRouteInfo(routeNumber, direction)
        return Route.Elements(
            buses.items, routeInfo.stops, routeInfo.shapePoints
        )
    }

    private fun requestBuses(routeNumber: Int, direction: Direction): Buses {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getBuses(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("Buses request failed with code ${response.code()}")
        }
        return response.body()!!
    }

    private fun requestRouteInfo(routeNumber: Int, direction: Direction): RouteInfo {
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

    private fun getForwardDirectionCode(direction: Direction): Int {
        return when (direction) {
            Direction.Forward -> 1
            Direction.Backward -> 0
        }
    }
}