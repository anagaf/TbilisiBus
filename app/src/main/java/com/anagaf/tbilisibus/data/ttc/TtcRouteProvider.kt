package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.RouteShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TtcRouteProvider @Inject constructor(private val retrofitService: TtcRetrofitService) :
    RouteProvider {

    override suspend fun getRoute(routeNumber: Int): Route =
        withContext(Dispatchers.IO) {
            val buses = requestBuses(routeNumber)
            val stops = requestShape(routeNumber)
            Route(routeNumber, buses, stops)
        }

    private fun requestBuses(routeNumber: Int): Buses =
        requestBuses(routeNumber, Direction.Forward) +
                requestBuses(routeNumber, Direction.Backward)


    private fun requestBuses(routeNumber: Int, direction: Direction): Buses {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getBuses(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("Buses request failed with code ${response.code()}")
        }
        return response.body()!!
    }

    private fun requestShape(routeNumber: Int): RouteShape =
        requestShape(routeNumber, Direction.Forward) +
                requestShape(routeNumber, Direction.Backward)

    private fun requestShape(routeNumber: Int, direction: Direction): RouteShape {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getRouteShape(routeNumber, forward).execute()
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