package com.anagaf.tbilisibus.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.anagaf.tbilisibus.data.Direction
import kotlinx.datetime.Instant

@Entity(tableName = "RouteInfo")
data class RouteInfoEntity(
    @PrimaryKey val routeNumber: Int,
    val timestamp: Instant
)

@Entity(tableName = "Stops",)
data class StopEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val routeNumber: Int = 0,
    val direction: Direction,
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "ShapePoints")
data class ShapePointEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val routeNumber: Int = 0,
    val direction: Direction,
    val latitude: Double,
    val longitude: Double
)

data class RouteInfoWithStopsAndShapePoints(
    val routeInfo: RouteInfoEntity,
    val stops: List<StopEntity>,
    val shapePoints: List<ShapePointEntity>
)

class InstantConverters {
    @TypeConverter
    fun instantFromLong(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @TypeConverter
    fun longToInstant(instant: Instant): Long = instant.toEpochMilliseconds()
}