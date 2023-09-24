package com.anagaf.tbilisibus.data

import com.anagaf.tbilisibus.data.cache.RouteInfoCache
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val kRouteNumber = 308

private val kBuses: BusesByDirection = mapOf(
    Direction.Forward to listOf(Bus(LatLng(5.0, 6.0))),
    Direction.Backward to listOf(Bus(LatLng(7.0, 8.0)))
)

private val kCachedStops: StopsByDirection = mapOf(
    Direction.Forward to listOf(Stop(LatLng(11.0, 22.0))),
    Direction.Backward to listOf(Stop(LatLng(33.0, 44.0)))
)
private val kRemoteStops: StopsByDirection = mapOf(
    Direction.Forward to listOf(Stop(LatLng(55.0, 66.0))),
    Direction.Backward to listOf(Stop(LatLng(77.0, 88.0)))
)

private val kCachedShapePoints: ShapePointsByDirection = mapOf(
    Direction.Forward to listOf(ShapePoint(LatLng(111.0, 222.0))),
    Direction.Backward to listOf(ShapePoint(LatLng(333.0, 444.0)))
)
private val kRemoteShapePoints: ShapePointsByDirection = mapOf(
    Direction.Forward to listOf(ShapePoint(LatLng(555.0, 666.0))),
    Direction.Backward to listOf(ShapePoint(LatLng(777.0, 888.0)))
)

private val kCachedRouteInfo = RouteInfo(kCachedStops, kCachedShapePoints)
private val kRemoteRouteInfo = RouteInfo(kRemoteStops, kRemoteShapePoints)

class RouteRepositoryTest {

    private val busesDataSource: BusesDataSource = mockk()
    private val routeInfoDataSource: RouteInfoDataSource = mockk()
    private val routeInfoCache: RouteInfoCache = mockk(relaxed = true)

    private lateinit var repo: RouteRepository

    @BeforeEach
    fun setUp() {
        repo = RouteRepositoryImpl(busesDataSource, routeInfoDataSource, routeInfoCache)

        coEvery { busesDataSource.getBuses(kRouteNumber) } returns kBuses
        coEvery { routeInfoDataSource.getRouteInfo(kRouteNumber) } returns kRemoteRouteInfo
    }

    @Test
    fun `provide cached route info if available`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { routeInfoCache.getRouteInfo(kRouteNumber) } returns kCachedRouteInfo

            val route = repo.getRoute(kRouteNumber)

            val expectedRoute = makeExpectedRoute(kCachedRouteInfo)

            assertEquals(expectedRoute, route)
        }

    @Test
    fun `provide remote route info if cached not available`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { routeInfoCache.getRouteInfo(kRouteNumber) } returns null

            val route = repo.getRoute(kRouteNumber)

            val expectedRoute = makeExpectedRoute(kRemoteRouteInfo)

            assertEquals(expectedRoute, route)

            coVerify { routeInfoCache.setRouteInfo(kRouteNumber, kRemoteRouteInfo) }
        }

    private fun makeExpectedRoute(routeInfo: RouteInfo): Route =
        Route(
            number = kRouteNumber,
            forward = Route.Elements(
                buses = kBuses[Direction.Forward]!!,
                stops = routeInfo.stops[Direction.Forward]!!,
                shapePoints = routeInfo.shapePoints[Direction.Forward]!!
            ),
            backward = Route.Elements(
                buses = kBuses[Direction.Backward]!!,
                stops = routeInfo.stops[Direction.Backward]!!,
                shapePoints = routeInfo.shapePoints[Direction.Backward]!!
            )
        )
}
