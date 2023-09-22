package com.anagaf.tbilisibus.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RouteInfoDao {

    @Transaction
    @Query("SELECT * FROM RouteInfo WHERE routeNumber = :routeNumber")
    fun get(routeNumber: Int): RouteInfoWithStopsAndShapePointsEntity

    @Transaction
    @Query("DELETE FROM RouteInfo WHERE routeNumber = :routeNumber")
    fun delete(routeNumber: Int)

    @Transaction
    @Insert
    fun insert(routeInfo: RouteInfoWithStopsAndShapePointsEntity)

}