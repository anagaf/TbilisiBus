package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.RouteRepository
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val kCitySouthWest = LatLng(10.0, 10.0)
private val kCityNorthEast = LatLng(20.0, 20.0)
private val kCityCenter = LatLng(15.0, 15.0)

private val kLocationOutsideCity = LatLng(30.0, 30.0)
private val kLocationInsideCity = LatLng(12.0, 12.0)

private val kTestCameraPosition =
    CameraPosition.builder().target(kLocationOutsideCity).zoom(4f).build()

private const val kRouteNumber = 123

private fun makeBus(index: Int) = Bus(LatLng(index.toDouble(), (index + 1).toDouble()))
private fun makeStop(index: Int) = Stop(LatLng(index.toDouble(), (index + 1).toDouble()))
private fun makeShapePoint(index: Int) =
    ShapePoint(LatLng(index.toDouble(), (index + 1).toDouble()))

private val kRoute = Route(
    number = kRouteNumber, forward = Route.Elements(
        buses = listOf(makeBus(11), makeBus(12)),
        stops = listOf(makeStop(11), makeStop(12)),
        shapePoints = listOf(makeShapePoint(11), makeShapePoint(12))
    ), backward = Route.Elements(
        buses = listOf(makeBus(13), makeBus(14)),
        stops = listOf(makeStop(13), makeStop(44)),
        shapePoints = listOf(makeShapePoint(13), makeShapePoint(14))
    )
)

private val kNewRoute = Route(
    number = kRouteNumber, forward = Route.Elements(
        buses = listOf(makeBus(5), makeBus(6)),
        stops = listOf(makeStop(5), makeStop(6)),
        shapePoints = listOf(makeShapePoint(5), makeShapePoint(6))
    ), backward = Route.Elements(
        buses = listOf(makeBus(7), makeBus(8)),
        stops = listOf(makeStop(7), makeStop(8)),
        shapePoints = listOf(makeShapePoint(7), makeShapePoint(8))
    )
)

private val kCurrentTime = Instant.fromEpochSeconds(1000)

private val kRouteNumberTtl = 5.minutes
private val kRouteReloadPeriod = 1.seconds

private val kRouteTtl = 1.minutes
private val kLocationTimeout = 1.seconds

private val kInitialCameraPosition = CameraPosition.Builder().target(kCityCenter).zoom(12f).build()

private val kDefaultUiState =
    MapUiState(alignment = UiAlignment.Right, cameraPosition = kInitialCameraPosition)

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MapViewModelTest.TraceUnitExtension::class)
class MapViewModelTest {

    private val routeRepository: RouteRepository = mockk()

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
        every { appDataStore.uiAlignment } returns UiAlignment.Right

        every { preferences.routeNumberTtl } returns kRouteNumberTtl
        every { preferences.routeReloadPeriod } returns kRouteReloadPeriod
        every { preferences.routeTtl } returns kRouteTtl
        every { preferences.locationTimeout } returns kLocationTimeout
        every { preferences.cityBounds } returns LatLngBounds(kCitySouthWest, kCityNorthEast)

        viewModel =
            MapViewModel(routeRepository, appDataStore, preferences, timeProvider, locationProvider)
    }

    private fun verifyUiState(expectedBuilder: () -> MapUiState) {
        val expected = expectedBuilder()
        assertEquals(expected, viewModel.uiState.value)
    }

    private suspend fun waitForUiState(expectedBuilder: () -> MapUiState) {
        val expected = expectedBuilder()
        val actual = viewModel.uiState.drop(1).first()
        assertEquals(actual, expected)
    }

    private fun makeBounds(route: Route, location: LatLng? = null): LatLngBounds {
        var bounds = route.bounds
        if (location != null) {
            bounds = bounds.including(location)
        }
        return bounds
    }

    private fun makeRouteUiState(route: Route, location: LatLng? = null) = MapUiState(
        cameraPosition = kInitialCameraPosition,
        cameraBounds = makeBounds(route, location),
        route = route
    )

    private fun prepareRoute() {
        coEvery { routeRepository.getRoute(kRouteNumber) } returns (kRoute)
        viewModel.onRouteNumberChosen(kRouteNumber)
    }

    @Test
    fun `move camera to Tbilisi on the very first run`() {
        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition,
            )
        }
    }

    @Test
    fun `move camera to initial position and show route number dialog on map ready`() = runTest {
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println("$tag: $message")
            }
        })

        every { appDataStore.lastCameraPosition } returns kInitialCameraPosition
        every { preferences.locationTimeout } returns 300.milliseconds
        viewModel.onMapReady()
        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition,
                dialogRequired = MapUiState.Dialog.Route
            )
        }
    }

    @Test
    fun `move camera to location and show route number dialog on map ready`(
    ) = runTest {
        val lastLocation = LatLng(1.0, 2.0)
        val expectedCameraPosition = CameraPosition
            .Builder()
            .target(lastLocation)
            .zoom(MapViewModel.kMyLocationZoom)
            .build()

        coEvery { locationProvider.getLastLocation() } returns lastLocation
        viewModel.onMapReady()
        waitForUiState {
            MapUiState(
                cameraPosition = expectedCameraPosition,
                dialogRequired = MapUiState.Dialog.Route
            )
        }
    }

    @Test
    fun `store camera position on activity stop`() {
        val arg = slot<CameraPosition>()
        every { appDataStore.lastCameraPosition = capture(arg) } returns Unit
        viewModel.onCameraMove(kTestCameraPosition)
        viewModel.onActivityStop()
        assert(arg.captured == kTestCameraPosition)
    }

    enum class LocationTestCase {
        AvailableInsideCity, AvailableOutsideCity, NotAvailable
    }

    @ParameterizedTest
    @EnumSource(LocationTestCase::class)
    @Timeout(3)
    fun `retrieve route from provider on route number chosen`(locationTestCase: LocationTestCase) =
        runTest(UnconfinedTestDispatcher()) {
            val invalidLocation = LatLng(50.0, 50.0)

            val location =
                when (locationTestCase) {
                    LocationTestCase.AvailableInsideCity -> kLocationInsideCity
                    LocationTestCase.AvailableOutsideCity -> kLocationOutsideCity
                    LocationTestCase.NotAvailable -> invalidLocation
                }

            coEvery { locationProvider.getLastLocation() } coAnswers {
                if (locationTestCase == LocationTestCase.NotAvailable) {
                    TimeUnit.SECONDS.sleep((kLocationTimeout + 1.seconds).inWholeSeconds)
                }
                location
            }

            val storedLastRouteNumberRequestTime = slot<Instant>()

            every {
                appDataStore.lastRouteNumberRequestTime = capture(storedLastRouteNumberRequestTime)
            } returns Unit

            every { timeProvider.now } returns kCurrentTime

            coEvery { routeRepository.getRoute(kRouteNumber) } returns kRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            verifyUiState {
                makeRouteUiState(
                    kRoute,
                    if (locationTestCase == LocationTestCase.AvailableInsideCity) location else null
                )
            }

            viewModel.onActivityStop()
        }

    @Test
    fun `reload current route`() = runTest(UnconfinedTestDispatcher()) {
        val firstRequestTime = kCurrentTime
        val secondRequestTime = firstRequestTime + 1.seconds
        every { timeProvider.now } returnsMany listOf(
            firstRequestTime, secondRequestTime
        )

        val storedLastRouteNumberRequestTime = slot<Instant>()

        every {
            appDataStore.lastRouteNumberRequestTime = capture(storedLastRouteNumberRequestTime)
        } returns Unit

        coEvery { routeRepository.getRoute(kRouteNumber) } returnsMany listOf(
            kRoute, kNewRoute
        )

        viewModel.onRouteNumberChosen(kRouteNumber)
        viewModel.onReloadRouteButtonClicked()

        verifyUiState {
            makeRouteUiState(kNewRoute)
        }

        coVerify(exactly = 2) { routeRepository.getRoute(kRouteNumber) }

        assertEquals(secondRequestTime, storedLastRouteNumberRequestTime.captured)

        viewModel.onActivityStop()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `show route number dialog on map ready if route number TTL passed`(
        lastRouteNumberRequestTimeAvailable: Boolean
    ) = runTest(UnconfinedTestDispatcher()) {
        every { timeProvider.now } returnsMany listOf(
            kCurrentTime, kCurrentTime + kRouteNumberTtl + 1.seconds
        )

        prepareRoute()

        every { appDataStore.lastRouteNumberRequestTime } returns if (lastRouteNumberRequestTimeAvailable) kCurrentTime else null

        viewModel.onMapReady()

        verifyUiState {
            makeRouteUiState(kRoute).copy(dialogRequired = MapUiState.Dialog.Route)
        }

        viewModel.onActivityStop()
    }

    @Test
    fun `reload route on map ready if route TTL passed`() = runTest(UnconfinedTestDispatcher()) {
        val firstRequestTime = kCurrentTime
        val secondRequestTime = firstRequestTime + kRouteTtl + 1.seconds

        every { timeProvider.now } returnsMany listOf(
            firstRequestTime, secondRequestTime, secondRequestTime + 1.seconds
        )

        coEvery { routeRepository.getRoute(kRouteNumber) } returnsMany listOf(
            kRoute, kNewRoute
        )

        viewModel.onRouteNumberChosen(kRouteNumber)

        every { appDataStore.lastRouteNumberRequestTime } returns firstRequestTime

        viewModel.onMapReady()

        verifyUiState {
            makeRouteUiState(kNewRoute)
        }

        coVerify(exactly = 2) { routeRepository.getRoute(kRouteNumber) }

        viewModel.onActivityStop()
    }

    @Test
    fun `not reload route on map ready if route TTL not passed`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstRequestTime = kCurrentTime
            val secondRequestTime = firstRequestTime + kRouteTtl - 1.seconds

            every { timeProvider.now } returnsMany listOf(
                firstRequestTime, secondRequestTime, secondRequestTime + 1.seconds
            )

            coEvery { routeRepository.getRoute(kRouteNumber) } returns kRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            every { appDataStore.lastRouteNumberRequestTime } returns firstRequestTime

            viewModel.onMapReady()

            verifyUiState {
                makeRouteUiState(kRoute)
            }

            coVerify(exactly = 1) { routeRepository.getRoute(kRouteNumber) }

            viewModel.onActivityStop()
        }

    @Test
    fun `show error message if cannot retrieve route`() = runTest(UnconfinedTestDispatcher()) {
        val ex = RuntimeException("Test exception")

        coEvery { routeRepository.getRoute(kRouteNumber) } throws ex

        viewModel.onRouteNumberChosen(kRouteNumber)

        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition, error = MapUiState.Error.RouteNotAvailable
            )
        }
    }

    @Test
    fun `hide dialog been dismissed`() {
        viewModel.onDialogDismissed()
        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition, dialogRequired = null
            )
        }
    }

    @Test
    fun `zoom to show whole route`() = runTest(UnconfinedTestDispatcher()) {
        every { timeProvider.now } returns kCurrentTime

        prepareRoute()

        viewModel.onZoomToShowRouteButtonClicked()

        verifyUiState {
            makeRouteUiState(kRoute)
        }

        viewModel.onActivityStop()
    }

    @Test
    fun `show route number dialog on button clicked`() {
        viewModel.onChooseRouteButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition, dialogRequired = MapUiState.Dialog.Route
            )
        }
    }

    @Test
    fun `show About dialog`() {
        viewModel.onAboutButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = kInitialCameraPosition, dialogRequired = MapUiState.Dialog.About
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `move camera to my location`(location_inside_tbilisi: Boolean) {
        val location = if (location_inside_tbilisi) kLocationInsideCity else kLocationOutsideCity

        coEvery { locationProvider.getLastLocation() } returns location
        viewModel.onMyLocationButtonClicked()
        verifyUiState {
            MapUiState(
                cameraPosition = CameraPosition.builder().target(location)
                    .zoom(MapViewModel.kMyLocationZoom).build(),
                dialogRequired = if (location_inside_tbilisi) null else MapUiState.Dialog.OutOfTbilisi
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
                cameraPosition = kInitialCameraPosition,
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
                cameraPosition = kInitialCameraPosition,
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `reload route periodically`(routeRequestSuccess: Boolean) =
        runTest(UnconfinedTestDispatcher()) {
            every { timeProvider.now } returns kCurrentTime

            coEvery { routeRepository.getRoute(kRouteNumber) } returns kRoute

            viewModel.onRouteNumberChosen(kRouteNumber)

            val startTime = Clock.System.now()
            val expectedCount = 3
            val timeout = kRouteReloadPeriod * (expectedCount + 1)
            var count = 0

            coEvery { routeRepository.getRoute(kRouteNumber) } answers {
                ++count
                if (!routeRequestSuccess) {
                    throw RuntimeException("Test")
                }
                kNewRoute
            }

            viewModel.onActivityStart()

            while (count < expectedCount && Clock.System.now() - startTime < timeout) {
                delay(1.seconds)
            }

            viewModel.onActivityStop()

            assertTrue(count == expectedCount)

            val expectedUtState = MapUiState(
                cameraPosition = kInitialCameraPosition,
                // route periodic updates don't cause bounds changes
                cameraBounds = makeBounds(kRoute),
                // route periodic updates failures are just ignored
                route = if (routeRequestSuccess) kNewRoute else kRoute
            )

            assertEquals(expectedUtState, viewModel.uiState.value)
        }

    @ParameterizedTest
    @EnumSource(UiAlignment::class)
    fun `update UI alignment on setting dialog confirmed`(uiAlignment: UiAlignment) {
        viewModel.onSettingsDialogConfirmed(uiAlignment)
        verifyUiState {
            kDefaultUiState.copy(alignment = uiAlignment, dialogRequired = null)
        }
        verify(exactly = 1) {
            appDataStore.uiAlignment = uiAlignment
        }
    }

    @Test
    fun `show settings dialog`() {
        viewModel.onSettingsButtonClicked()
        verifyUiState {
            kDefaultUiState.copy(dialogRequired = MapUiState.Dialog.Settings)
        }
    }

    @Test
    fun `move camera to Tbilisi`() {
        viewModel.moveCameraToTbilisi()
        verifyUiState {
            kDefaultUiState.copy(cameraPosition = kInitialCameraPosition)
        }
    }
}