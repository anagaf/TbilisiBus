package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.RouteInfo
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.ui.TimeProvider
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val kRouteInfoTtl = 1.minutes
private const val kRouteNumber = 311
private val kStopPosition1 = LatLng(1.0, 2.0)
private val kStopPosition2 = LatLng(3.0, 4.0)
private val kShapePointPosition1 = LatLng(5.0, 6.0)
private val kShapePointPosition2 = LatLng(7.0, 8.0)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteInfoCacheTest {

    private val routeInfoDao: RouteInfoDao = mockk()

    private val timeProvider: TimeProvider = mockk()

    private lateinit var cache: RouteInfoCache

    @BeforeEach
    fun setUp() {
        cache = RouteInfoCacheImpl(routeInfoDao, timeProvider, kRouteInfoTtl)
    }

    @Test
    fun shouldReturnRouteInfo() =
        runTest(UnconfinedTestDispatcher()) {
            val timestamp = Instant.fromEpochMilliseconds(1000)
            val routeInfoEntity = RouteInfoWithStopsAndShapePoints(
                routeInfo = RouteInfoEntity(kRouteNumber, timestamp),
                stops = listOf(
                    makeStopEntity(kRouteNumber, Direction.Forward, kStopPosition1)
                ),
                shapePoints = listOf(
                    makeShapePointEntity(kRouteNumber, Direction.Forward, kShapePointPosition1)
                )
            )
            val expectedRouteInfo = RouteInfo(
                stops = mapOf(Direction.Forward to listOf(Stop(kStopPosition1))),
                shapePoints = mapOf(Direction.Forward to listOf(ShapePoint(kShapePointPosition1)))
            )

            coEvery { timeProvider.now } returns timestamp + kRouteInfoTtl - 1.seconds

            coEvery { routeInfoDao.get(kRouteNumber) } returns routeInfoEntity

            val routeInfo = cache.getRouteInfo(kRouteNumber)
            assertEquals(routeInfo, expectedRouteInfo)
        }


    private fun makeStopEntity(routeNumber: Int, direction: Direction, pos: LatLng) =
        StopEntity(
            routeNumber = routeNumber,
            direction = direction,
            latitude = pos.latitude,
            longitude = pos.longitude
        )

    private fun makeShapePointEntity(routeNumber: Int, direction: Direction, pos: LatLng) =
        ShapePointEntity(
            routeNumber = routeNumber,
            direction = direction,
            latitude = pos.latitude,
            longitude = pos.longitude
        )
}