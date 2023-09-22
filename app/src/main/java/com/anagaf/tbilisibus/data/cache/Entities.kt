package com.anagaf.tbilisibus.data.cache

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.anagaf.tbilisibus.data.Direction
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

@Entity(tableName = "RouteInfo")
@OptIn(ExperimentalTime::class)
data class RouteInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val routeNumber: Int,
    val timestamp: TimeMark
)

@Entity(
    tableName = "Stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeInfoId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val routeInfoId: Int,
    val direction: Direction,
    val latitude: Double,
    val longitude: Double
)

@Entity(
    tableName = "ShapePoints",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = RouteInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeInfoId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
            onUpdate = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class ShapePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val routeInfoId: Int,
    val direction: Direction,
    val latitude: Double,
    val longitude: Double
)

data class RouteInfoWithStopsAndShapePointsEntity(
    @Embedded
    val routeInfo: RouteInfoEntity,
    @Relation(parentColumn = "id", entityColumn = "routeInfoId")
    val stops: List<StopEntity>,
    @Relation(parentColumn = "id", entityColumn = "routeInfoId")
    val shapePoints: List<ShapePointEntity>
)
