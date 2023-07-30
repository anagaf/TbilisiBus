package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


private val kTestCameraPosition = CameraPosition.builder().target(LatLng(11.0, 12.0))
    .zoom(4f)
    .build()

private const val kRouteNumber = 123

private fun makeBus(index: Int) = Bus(LatLng(index.toDouble(), (index + 1).toDouble()))
private fun makeStop(index: Int) = Stop(LatLng(index.toDouble(), (index + 1).toDouble()))
private fun makeShapePoint(index: Int) =
    ShapePoint(LatLng(index.toDouble(), (index + 1).toDouble()))

private val kRoute =
    Route(
        number = kRouteNumber,
        forward = Route.Elements(
            buses = listOf(makeBus(1), makeBus(2)),
            stops = listOf(makeStop(1), makeStop(2)),
            shapePoints = listOf(makeShapePoint(1), makeShapePoint(2))
        ),
        backward = Route.Elements(
            buses = listOf(makeBus(3), makeBus(4)),
            stops = listOf(makeStop(3), makeStop(4)),
            shapePoints = listOf(makeShapePoint(3), makeShapePoint(4))
        )
    )

private val kNewRoute =
    Route(
        number = kRouteNumber,
        forward = Route.Elements(
            buses = listOf(makeBus(5), makeBus(6)),
            stops = listOf(makeStop(5), makeStop(6)),
            shapePoints = listOf(makeShapePoint(5), makeShapePoint(6))
        ),
        backward = Route.Elements(
            buses = listOf(makeBus(7), makeBus(8)),
            stops = listOf(makeStop(7), makeStop(8)),
            shapePoints = listOf(makeShapePoint(7), makeShapePoint(8))
        )
    )

private val kCurrentTime = Instant.fromEpochSeconds(1000)

private val kRouteNumberTtl = 5.minutes
private val kRouteTtl = 1.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MapViewModelTest.TraceUnitExtension::class)
class MapViewModelTest {

    private val routeProvider: RouteProvider = mockk()

    private val appDataStore: AppDataStore = mockk(relaxUnitFun = true)

    private val preferences: Preferences = mockk()

    private val timeProvider: TimeProvider = mockk()

    private val locationProvider: LocationProvider = mockk()

    private lateinit var viewModel: MapViewModel

    class TraceUnitExtension : AfterEachCallback, BeforeEachCallback {
        @Throws(Exception::class)
        override fun beforeEach(context: ExtensionContext) {
            Dispatchers.setMain(UnconfinedTestDispatcher())
        }

        @Throws(Exception::class)
        override fun afterEach(context: ExtensionContext?) {
            Dispatchers.resetMain()
        }
    }

    @BeforeEach
    fun setUp() {
        every { appDataStore.lastCameraPosition } returns null
        viewModel =
            MapViewModel(routeProvider, appDataStore, preferences, timeProvider, locationProvider)

        every { preferences.routeNumberTtl } returns kRouteNumberTtl
        every { preferences.routeTtl } returns kRouteTtl
    }

    private fun verifyUiState(expectedBuilder: () -> MapUiState) {
        val expected = expectedBuilder()
        assertEquals(expected, viewModel.uiState.value)
    }

    private fun prepareRoute() {
        coEvery { routeProvider.getRoute(kRouteNumber) } returns (kRoute)
        viewModel.onRouteNumberChosen(kRouteNumber)
    }

    @Test
    fun `move camera to Tbilisi on the very first run`() {
        verifyUiState {
            MapUiState(
                cameraPosition = MapViewModel.kInitialCameraPosition,
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `restore last camera position and show route number dialog on map ready`(
        lastCameraPositionAvailable: Boolean
    ) {
        val lastCameraPosition = if (lastCameraPositionAvailable) kTestCameraPosition else null
        every { appDataStore.lastCameraPosition } returns lastCameraPosition
        viewModel.onMapReady()
        verifyUiState {
            MapUiState(
                cameraPosition = lastCameraPosition ?: MapViewModel.kInitialCameraPosition,
                routeNumberDialogRequired = true
            )
        }
    }

    @Test
    fun `store camera position on camera move`() {
        val arg = slot<CameraPosition>()
        every { appDataStore.lastCameraPosition = capture(arg) } returns Unit
        viewModel.onCameraMove(kTestCameraPosition)
        assert(arg.captured == kTestCameraPosition)
    }

    @Test
    fun `retrieve route from provider on route number chosen`() =
        runTest(UnconfinedTestDispatcher()) {
            val storedLastRouteNumberRequestTime = slot<Instant>()

            every {
                appDataStore.lastRouteNumberRequestTime =
                    capture(storedLastRouteNumberRequestTime)
            } returns Unit

            every { timeProvider.now } returns kCurrentTime

            coEvery { routeProvider.getRoute(kRouteNumber) } returns kRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kRoute
                )
            }
        }

    @Test
    fun `reload current route`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTime
            val secondRequestTime = firstRequestTime + 1.seconds
            every { timeProvider.now } returnsMany listOf(
                firstRequestTime,
                secondRequestTime
            )

            val storedLastRouteNumberRequestTime = slot<Instant>()

            every {
                appDataStore.lastRouteNumberRequestTime =
                    capture(storedLastRouteNumberRequestTime)
            } returns Unit

            coEvery { routeProvider.getRoute(kRouteNumber) } returnsMany listOf(
                kRoute,
                kNewRoute
            )

            viewModel.onRouteNumberChosen(kRouteNumber)
            viewModel.onReloadRouteButtonClicked()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kNewRoute
                )
            }

            coVerify(exactly = 2) { routeProvider.getRoute(kRouteNumber) }

            assertEquals(secondRequestTime, storedLastRouteNumberRequestTime.captured)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `show route number dialog on map ready if route number TTL passed`(
        lastRouteNumberRequestTimeAvailable: Boolean
    ) =
        runTest(UnconfinedTestDispatcher()) {
            every { timeProvider.now } returnsMany listOf(
                kCurrentTime,
                kCurrentTime + kRouteNumberTtl + 1.seconds
            )

            prepareRoute()

            every { appDataStore.lastRouteNumberRequestTime } returns
                    if (lastRouteNumberRequestTimeAvailable) kCurrentTime else null

            viewModel.onMapReady()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kRoute,
                    routeNumberDialogRequired = true
                )
            }
        }

    @Test
    fun `reload route on map ready if route TTL passed`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTime
            val secondRequestTime = firstRequestTime + kRouteTtl + 1.seconds

            every { timeProvider.now } returnsMany listOf(
                firstRequestTime,
                secondRequestTime,
                secondRequestTime + 1.seconds
            )

            coEvery { routeProvider.getRoute(kRouteNumber) } returnsMany listOf(
                kRoute,
                kNewRoute
            )

            viewModel.onRouteNumberChosen(kRouteNumber)

            every { appDataStore.lastRouteNumberRequestTime } returns firstRequestTime

            viewModel.onMapReady()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kNewRoute,
                )
            }

            coVerify(exactly = 2) { routeProvider.getRoute(kRouteNumber) }
        }

    @Test
    fun `not reload route on map ready if route TTL not passed`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTime
            val secondRequestTime = firstRequestTime + kRouteTtl - 1.seconds

            every { timeProvider.now } returnsMany listOf(
                firstRequestTime,
                secondRequestTime,
                secondRequestTime + 1.seconds
            )

            coEvery { routeProvider.getRoute(kRouteNumber) } returns kRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            every { appDataStore.lastRouteNumberRequestTime } returns firstRequestTime

            viewModel.onMapReady()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kRoute,
                )
            }

            coVerify(exactly = 1) { routeProvider.getRoute(kRouteNumber) }
        }

    @Test
    fun `show error message if cannot retrieve route`() =
        runTest(UnconfinedTestDispatcher()) {
            val ex = RuntimeException("Test exception")

            coEvery { routeProvider.getRoute(kRouteNumber) } throws ex

            viewModel.onRouteNumberChosen(kRouteNumber)

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    error = MapUiState.Error.RouteNotAvailable
                )
            }
        }

    @Test
    fun `hide route number dialog been dismissed`() {
        viewModel.onRouteNumberChangeDismissed()
        verifyUiState {
            MapUiState(
                cameraPosition = MapViewModel.kInitialCameraPosition,
                routeNumberDialogRequired = false
            )
        }
    }

    @Test
    fun `zoom to show whole route`() =
        runTest(UnconfinedTestDispatcher()) {
            every { timeProvider.now } returns kCurrentTime

            prepareRoute()

            viewModel.onZoomToShowRouteButtonClicked()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    route = kRoute,
                    cameraBounds = kRoute.bounds
                )
            }
        }

    @Test
    fun `show route number dialog on button clicked`() {
        viewModel.onChooseRouteButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = MapViewModel.kInitialCameraPosition,
                routeNumberDialogRequired = true
            )
        }
    }

    @Test
    fun `move camera to my location`() {
        coEvery { locationProvider.getLastLocation() } returns kTestCameraPosition.target
        viewModel.onMyLocationButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = CameraPosition.builder()
                    .target(kTestCameraPosition.target)
                    .zoom(MapViewModel.kInitialCameraPosition.zoom)
                    .build(),
            )
        }
    }

    @Test
    fun `show error message if my location not available`() {
        val ex = RuntimeException("Test exception")
        coEvery { locationProvider.getLastLocation() } throws ex
        viewModel.onMyLocationButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = MapViewModel.kInitialCameraPosition,
                error = MapUiState.Error.LocationNotAvailable
            )
        }
    }

    @Test
    fun `reset error message after been shown`() {
        val ex = RuntimeException("Test exception")
        coEvery { locationProvider.getLastLocation() } throws ex
        viewModel.onMyLocationButtonClicked()
        viewModel.onErrorMessageShown()
        verifyUiState {
            MapUiState(
                cameraPosition = MapViewModel.kInitialCameraPosition,
            )
        }
    }
}