package com.anagaf.tbilisibus.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RouteInfoDao {

    @Transaction
    @Query("SELECT * FROM RouteInfo WHERE routeNumber = :routeNumber")
    abstract fun get(routeNumber: Int): RouteInfoWithStopsAndShapePointsEntity?

    @Transaction
    @Query("DELETE FROM RouteInfo WHERE routeNumber = :routeNumber")
    abstract fun delete(routeNumber: Int)

    @Insert
    abstract fun insertRouteInfo(routeInfo: RouteInfoEntity)

    @Insert
    abstract fun insertStops(stops: List<StopEntity>)

    @Insert
    abstract fun insertShapePoints(shapePoint: List<ShapePointEntity>)

    @Transaction
    @Query("")
    fun insert(routeInfo: RouteInfoWithStopsAndShapePointsEntity) {
        delete(routeInfo.routeInfo.routeNumber)
        insertRouteInfo(routeInfo.routeInfo)
        insertStops(routeInfo.stops)
        insertShapePoints(routeInfo.shapePoints)
    }
}