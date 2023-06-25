package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.RouteShape
import com.anagaf.tbilisibus.data.Stop
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
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


private val kTestCameraPosition = CameraPosition.builder().target(LatLng(11.0, 12.0))
    .zoom(4f)
    .build()

private const val kRouteNumber = 123

private val kTestStop =
    Stop(id = 123, location = Location(3.0, 3.0), direction = Direction.Forward)

private val kTestBus =
    Bus(
        location = Location(1.0, 2.0),
        direction = Direction.Forward,
        nextStopId = kTestStop.id
    )

private val kNewTestBus =
    Bus(
        location = Location(6.0, 7.0),
        direction = Direction.Forward,
        nextStopId = kTestStop.id
    )

private val kTestRoute =
    Route(
        number = kRouteNumber,
        buses = Buses(listOf(kTestBus)),
        shape = RouteShape(
            stops = listOf(kTestStop),
            points = listOf()
        ),
    )

private
val kNewTestRoute =
    Route(
        number = kRouteNumber,
        buses = Buses(listOf(kNewTestBus)),
        shape = RouteShape(
            stops = listOf(kTestStop),
            points = listOf()
        ),
    )

private
val kTestMarkers = listOf(
    MapUiState.Marker(
        type = MapUiState.Marker.Type.Bus,
        location = kTestBus.location.latLng,
        direction = kTestBus.direction,
        heading = null
    ),
    MapUiState.Marker(
        type = MapUiState.Marker.Type.Stop,
        location = kTestStop.location.latLng,
        direction = kTestStop.direction,
        heading = null
    )
)

private
val kNewMarkers = listOf(
    MapUiState.Marker(
        type = MapUiState.Marker.Type.Bus,
        location = kNewTestBus.location.latLng,
        direction = kNewTestBus.direction,
        heading = null
    ),
    MapUiState.Marker(
        type = MapUiState.Marker.Type.Stop,
        location = kTestStop.location.latLng,
        direction = kTestStop.direction,
        heading = null
    )
)

private const val kCurrentTimeMillis = 123456789L

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

        every { preferences.routeNumberTtl } returns kRouteTtl
    }

    private fun verifyUiState(expectedBuilder: () -> MapUiState) {
        assertEquals(expectedBuilder(), viewModel.uiState.value)
    }

    private fun prepareRoute() {
        coEvery { routeProvider.getRoute(kRouteNumber) } returns (kTestRoute)
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
            val storedLastRouteNumberRequestTimeInMillis = slot<Long>()

            every {
                appDataStore.lastRouteNumberRequestTimeInMillis =
                    capture(storedLastRouteNumberRequestTimeInMillis)
            } returns Unit

            every { timeProvider.currentTimeMillis } returns kCurrentTimeMillis

            coEvery { routeProvider.getRoute(kRouteNumber) } returns kTestRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    routeNumber = kRouteNumber,
                    routeMarkers = kTestMarkers,
                )
            }
        }

    @Test
    fun `reload current route`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTimeMillis
            val secondRequestTime = firstRequestTime + 1
            every { timeProvider.currentTimeMillis } returnsMany listOf(
                firstRequestTime,
                secondRequestTime
            )

            val storedLastRouteNumberRequestTimeInMillis = slot<Long>()

            every {
                appDataStore.lastRouteNumberRequestTimeInMillis =
                    capture(storedLastRouteNumberRequestTimeInMillis)
            } returns Unit

            coEvery { routeProvider.getRoute(kRouteNumber) } returnsMany listOf(
                kTestRoute,
                kNewTestRoute
            )

            viewModel.onRouteNumberChosen(kRouteNumber)
            viewModel.onReloadRouteButtonClicked()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    routeNumber = kRouteNumber,
                    routeMarkers = kNewMarkers,
                )
            }

            coVerify(exactly = 2) { routeProvider.getRoute(kRouteNumber) }

            assertEquals(secondRequestTime, storedLastRouteNumberRequestTimeInMillis.captured)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `show route number dialog on map ready if route number TTL passed`(
        lastRouteNumberRequestTimeAvailable: Boolean
    ) =
        runTest(UnconfinedTestDispatcher()) {
            every { timeProvider.currentTimeMillis } returnsMany listOf(
                kCurrentTimeMillis,
                kCurrentTimeMillis + kRouteTtl.inWholeMilliseconds + 1
            )

            prepareRoute()

            every { appDataStore.lastRouteNumberRequestTimeInMillis } returns
                    if (lastRouteNumberRequestTimeAvailable) kCurrentTimeMillis else null

            viewModel.onMapReady()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    routeNumber = kRouteNumber,
                    routeMarkers = kTestMarkers,
                    routeNumberDialogRequired = true
                )
            }
        }

    @Test
    fun `reload route on map ready if route number TTL not passed`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTimeMillis
            val secondRequestTime = firstRequestTime + kRouteTtl.inWholeMilliseconds - 1

            every { timeProvider.currentTimeMillis } returnsMany listOf(
                firstRequestTime,
                secondRequestTime,
                secondRequestTime + 1
            )

            coEvery { routeProvider.getRoute(kRouteNumber) } returnsMany listOf(
                kTestRoute,
                kNewTestRoute
            )

            viewModel.onRouteNumberChosen(kRouteNumber)

            every { appDataStore.lastRouteNumberRequestTimeInMillis } returns firstRequestTime

            viewModel.onMapReady()

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    routeNumber = kRouteNumber,
                    routeMarkers = kNewMarkers,
                )
            }

            coVerify(exactly = 2) { routeProvider.getRoute(kRouteNumber) }
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
            every { timeProvider.currentTimeMillis } returns kCurrentTimeMillis

            prepareRoute()

            viewModel.onZoomToShowRouteButtonClicked()

            val expectedCameraBounds = kTestMarkers.let {
                LatLngBounds.builder().apply {
                    it.forEach { include(it.location) }
                }.build()
            }

            verifyUiState {
                MapUiState(
                    cameraPosition = MapViewModel.kInitialCameraPosition,
                    routeNumber = kRouteNumber,
                    routeMarkers = kTestMarkers,
                    cameraBounds = expectedCameraBounds
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