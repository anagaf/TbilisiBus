package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.BusesByDirection
import com.anagaf.tbilisibus.data.BusesDataSource
import com.anagaf.tbilisibus.data.Direction
import javax.inject.Inject

class TtcBusesDataSource @Inject constructor(private val retrofitService: TtcRetrofitService) :
    BusesDataSource {

    override suspend fun getBuses(routeNumber: Int): BusesByDirection =
        mapOf(
            Direction.Forward to requestBuses(routeNumber, Direction.Forward).buses,
            Direction.Backward to requestBuses(routeNumber, Direction.Backward).buses
        )

    private fun requestBuses(routeNumber: Int, direction: Direction): DirectionBuses {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getBuses(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("Buses request failed with code ${response.code()}")
        }
        return response.body()!!
    }
}
