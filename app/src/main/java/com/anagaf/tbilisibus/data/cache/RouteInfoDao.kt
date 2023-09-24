package com.anagaf.tbilisibus.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RouteInfoDao {

    @Query("SELECT * FROM RouteInfo WHERE routeNumber = :routeNumber")
    protected abstract suspend fun getRouteInfo(routeNumber: Int): RouteInfoEntity?

    @Query("SELECT * FROM Stops WHERE routeNumber = :routeNumber")
    protected abstract suspend fun getStops(routeNumber: Int): List<StopEntity>

    @Query("SELECT * FROM ShapePoints WHERE routeNumber = :routeNumber")
    protected abstract suspend fun getShapePoints(routeNumber: Int): List<ShapePointEntity>

    @Transaction
    @Query("")
    suspend fun get(routeNumber: Int): RouteInfoWithStopsAndShapePoints? {
        val routeInfo = getRouteInfo(routeNumber) ?: return null
        return RouteInfoWithStopsAndShapePoints(
            routeInfo,
            getStops(routeNumber),
            getShapePoints(routeNumber)
        )
    }

    @Query("DELETE FROM RouteInfo WHERE routeNumber = :routeNumber")
    protected abstract suspend fun deleteRouteInfo(routeNumber: Int)

    @Query("DELETE FROM Stops WHERE routeNumber = :routeNumber")
    protected abstract suspend fun deleteStops(routeNumber: Int)

    @Query("DELETE FROM ShapePoints WHERE routeNumber = :routeNumber")
    protected abstract suspend fun deleteShapePoints(routeNumber: Int)

    @Insert
    protected abstract suspend fun insertRouteInfo(routeInfo: RouteInfoEntity)

    @Insert
    protected abstract suspend fun insertStops(stops: List<StopEntity>)

    @Insert
    protected abstract suspend fun insertShapePoints(shapePoint: List<ShapePointEntity>)

    @Transaction
    @Query("")
    suspend fun insert(
        routeInfo: RouteInfoWithStopsAndShapePoints,
    ) {
        deleteStops(routeInfo.routeInfo.routeNumber)
        deleteShapePoints(routeInfo.routeInfo.routeNumber)
        deleteRouteInfo(routeInfo.routeInfo.routeNumber)

        insertRouteInfo(routeInfo.routeInfo)
        insertStops(routeInfo.stops)
        insertShapePoints(routeInfo.shapePoints)
    }
}