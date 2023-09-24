package com.anagaf.tbilisibus.data.cache

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.RouteInfo
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.ui.TimeProvider
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val ROUTE_NUMBER = 311

private val kRouteInfoTtl = 1.minutes
private val kStopPosition1 = LatLng(1.0, 2.0)
private val kStopPosition2 = LatLng(3.0, 4.0)
private val kShapePointPosition1 = LatLng(5.0, 6.0)
private val kShapePointPosition2 = LatLng(7.0, 8.0)
private val kTimestamp = Instant.fromEpochMilliseconds(1000)

private val kRouteInfo = RouteInfo(
    stops = mapOf(
        Direction.Forward to listOf(Stop(kStopPosition1)),
        Direction.Backward to listOf(Stop(kStopPosition2))
    ),
    shapePoints = mapOf(
        Direction.Forward to listOf(ShapePoint(kShapePointPosition1)),
        Direction.Backward to listOf(ShapePoint(kShapePointPosition2))
    )
)

private fun makeStopEntity(direction: Direction, pos: LatLng) =
    StopEntity(
        routeNumber = ROUTE_NUMBER,
        direction = direction,
        latitude = pos.latitude,
        longitude = pos.longitude
    )

private fun makeShapePointEntity(direction: Direction, pos: LatLng) =
    ShapePointEntity(
        routeNumber = ROUTE_NUMBER,
        direction = direction,
        latitude = pos.latitude,
        longitude = pos.longitude
    )

private val kDaoRouteInfo = RouteInfoWithStopsAndShapePoints(
    routeInfo = RouteInfoEntity(ROUTE_NUMBER, kTimestamp),
    stops = listOf(
        makeStopEntity(Direction.Forward, kStopPosition1),
        makeStopEntity(Direction.Backward, kStopPosition2),
    ),
    shapePoints = listOf(
        makeShapePointEntity(Direction.Forward, kShapePointPosition1),
        makeShapePointEntity(Direction.Backward, kShapePointPosition2),
    )
)


@OptIn(ExperimentalCoroutinesApi::class)
class RouteInfoCacheTest {

    private val routeInfoDao: RouteInfoDao = mockk(relaxed = true)

    private val timeProvider: TimeProvider = mockk()

    private lateinit var cache: RouteInfoCache

    @BeforeEach
    fun setUp() {
        cache = RouteInfoCacheImpl(routeInfoDao, timeProvider, kRouteInfoTtl)
    }

    @Test
    fun shouldReturnRouteInfo() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { routeInfoDao.get(ROUTE_NUMBER) } returns kDaoRouteInfo

            coEvery { timeProvider.now } returns kTimestamp + kRouteInfoTtl - 1.seconds

            assertEquals(cache.getRouteInfo(ROUTE_NUMBER), kRouteInfo)
        }

    @Test
    fun shouldReturnNullIfCachedRouteInfoExpired() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { routeInfoDao.get(ROUTE_NUMBER) } returns kDaoRouteInfo

            coEvery { timeProvider.now } returns kTimestamp + kRouteInfoTtl + 1.seconds

            assertNull(cache.getRouteInfo(ROUTE_NUMBER))
        }

    @Test
    fun shouldWriteRouteInfoToDao() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { timeProvider.now } returns kTimestamp

            cache.setRouteInfo(ROUTE_NUMBER, kRouteInfo)

            coVerify(exactly = 1) { routeInfoDao.insert(kDaoRouteInfo) }
        }
}