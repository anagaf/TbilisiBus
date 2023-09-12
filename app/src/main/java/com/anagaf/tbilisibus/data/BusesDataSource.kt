package com.anagaf.tbilisibus.data

interface BusesDataSource {
    suspend fun getBuses(routeNumber: Int): BusesByDirection
}