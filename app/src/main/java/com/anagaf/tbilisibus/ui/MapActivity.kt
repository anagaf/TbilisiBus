package com.anagaf.tbilisibus.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Route
import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

private const val CAMERA_ANIMATION_DURATION_MS = 1000

object MarkerIcons {
    lateinit var bus: Map<Direction, BitmapDescriptor>
    lateinit var stop: Map<Direction, BitmapDescriptor>

    fun init(context: Context) {
        bus = mapOf(
            Direction.Forward to makeMarkerDrawable(context, R.drawable.forward_bus_arrow),
            Direction.Backward to makeMarkerDrawable(context, R.drawable.backward_bus_arrow)
        )

        stop = mapOf(
            Direction.Forward to makeMarkerDrawable(context, R.drawable.forward_stop),
            Direction.Backward to makeMarkerDrawable(context, R.drawable.backward_stop)
        )
    }
}

interface MapButtonsClickHandler {
    fun onZoomIn()
    fun onZoomOut()
    fun onChooseRoute()
    fun onMyLocation()
    fun onShowRoute()
    fun onReloadRoute()
    fun onAbout()
}

@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Timber.i("Permission granted: $granted")
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            var isMapReady by remember { mutableStateOf(false) }

            val cameraPositionState = rememberCameraPositionState {
                position = uiState.cameraPosition
            }

            val locationPermissionState = rememberPermissionState(
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            if (isMapReady) {
                LaunchedEffect(uiState.cameraPosition, uiState.cameraBounds) {
                    if (uiState.cameraBounds != null) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(uiState.cameraBounds!!, 100),
                            CAMERA_ANIMATION_DURATION_MS
                        )
                    } else {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(uiState.cameraPosition),
                            CAMERA_ANIMATION_DURATION_MS
                        )
                    }
                }
            }

            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    viewModel.onCameraMove(cameraPositionState.position)
                }
            }


            LaunchedEffect(uiState.error) {
                if (uiState.error != null) {
                    val messageId = when (uiState.error!!) {
                        MapUiState.Error.RouteNotAvailable -> R.string.route_is_not_available
                        MapUiState.Error.LocationNotAvailable -> R.string.location_not_available
                    }
                    Toast.makeText(applicationContext, getString(messageId), Toast.LENGTH_LONG)
                        .show()
                    viewModel.onErrorMessageShown()
                }
            }

            LaunchedEffect(locationPermissionState) {
                Timber.d("Location permission status ${locationPermissionState.status}")
                if (!locationPermissionState.status.isGranted) {
                    locationPermissionState.launchPermissionRequest()
                }
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMapView(
                    route = uiState.route,
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    locationPermissionState = locationPermissionState,
                    onMapLoaded = {
                        isMapReady = true
                        MarkerIcons.init(this@MapActivity)
                        viewModel.onMapReady()
                    },
                )
            }

            if (isMapReady) {
                val coroutineScope = rememberCoroutineScope()
                val clickHandler = object : MapButtonsClickHandler {
                    override fun onZoomIn() {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                    }

                    override fun onZoomOut() {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                    }

                    override fun onChooseRoute() {
                        viewModel.onChooseRouteButtonClicked()
                    }

                    override fun onMyLocation() {
                        viewModel.onMyLocationButtonClicked()
                    }

                    override fun onShowRoute() {
                        viewModel.onZoomToShowRouteButtonClicked()
                    }

                    override fun onReloadRoute() {
                        viewModel.onReloadRouteButtonClicked()
                    }

                    override fun onAbout() {
                        viewModel.onAboutButtonClicked()
                    }
                }
                MapControlButtons(
                    routeAvailable = uiState.route != null,
                    clickHandler = clickHandler,
                    myLocationButtonEnabled = locationPermissionState.status.isGranted,
                    alignment = uiState.alignment,
                )

                when (uiState.dialogRequired) {
                    MapUiState.Dialog.Route ->
                        RouteNumberDialog(
                            onConfirmed = { routeNumber -> viewModel.onRouteNumberChosen(routeNumber) },
                            onDismissed = { viewModel.onDialogDismissed() })

                    MapUiState.Dialog.About -> AboutDialog(
                        onDismissed = { viewModel.onDialogDismissed() })

                    MapUiState.Dialog.OutOfTbilisi -> OutOfTbilisiDialog(
                        onMoveAccepted = { viewModel.moveCameraToTbilisi() },
                        onDismissed = { viewModel.onDialogDismissed() })

                    null -> {}
                }
            }

            if (!isMapReady || uiState.inProgress) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.route != null) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.default_padding))
                ) {
                    Text(
                        text = getString(R.string.title_format).format(uiState.route!!.number),
                        fontSize = 24.sp,
                        color = colorResource(id = R.color.map_control),
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isLocationPermissionGranted()) {
            requestLocationPermission()
        }
        viewModel.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onActivityStop()
    }

    private fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        Timber.d("Requesting location permission")
        permissionRequestLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GoogleMapView(
    route: Route?,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    locationPermissionState: PermissionState,
    onMapLoaded: () -> Unit = {},
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
        properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
    ) {
        if (route != null) {
            with(route.forward) {
                BusMarkers(buses, shapePoints, Direction.Forward)
                RouteShape(shapePoints, Direction.Forward)
                StopMarkers(stops, Direction.Forward, cameraPositionState)
            }

            with(route.backward) {
                BusMarkers(buses, shapePoints, Direction.Backward)
                RouteShape(shapePoints, Direction.Backward)
                StopMarkers(stops, Direction.Backward, cameraPositionState)
            }


        }
    }
}

@Composable
fun BusMarkers(buses: List<Bus>, shapePoints: List<ShapePoint>, direction: Direction) {
    for (bus in buses) {
        BusMarker(bus, shapePoints, direction)
    }
}

@Composable
fun StopMarkers(stops: List<Stop>, direction: Direction, cameraPositionState: CameraPositionState) {
    if (cameraPositionState.position.zoom > 14) {
        val visibleBounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
        for (stop in stops) {
            if (visibleBounds != null && visibleBounds.contains(stop.position)) {
                StopMarker(stop.position, direction)
            }
        }
    }
}

@Composable
private fun BusMarker(bus: Bus, shapePoints: List<ShapePoint>, direction: Direction) {
    val heading = calculateBusHeading(bus.position, shapePoints)
    Marker(
        state = MarkerState(position = bus.position),
        icon = MarkerIcons.bus[direction],
        rotation = heading?.toFloat() ?: 0f
    )
}

@Composable
fun StopMarker(position: LatLng, direction: Direction) {
    Marker(
        state = rememberMarkerState(position = position),
        icon = MarkerIcons.stop[direction],
    )
}

@Composable
fun RouteShape(shapePoints: List<ShapePoint>, direction: Direction) {
    val color = when (direction) {
        Direction.Forward -> LocalContext.current.getColor(R.color.forward_route)
        Direction.Backward -> LocalContext.current.getColor(R.color.backward_route)
    }
    Polyline(points = shapePoints.map { it.position }, color = Color(color))
}

@Composable
private fun MapControlButtons(
    routeAvailable: Boolean,
    clickHandler: MapButtonsClickHandler,
    myLocationButtonEnabled: Boolean,
    alignment: UiAlignment
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.default_padding)),
        horizontalAlignment = if (alignment == UiAlignment.Right) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.Bottom,
    ) {
        MapControlButton(
            onClick = { clickHandler.onChooseRoute() },
            drawableId = R.drawable.bus,
            contentDescriptionId = R.string.choose_route
        )
        if (routeAvailable) {
            MapControlButton(
                onClick = { clickHandler.onReloadRoute() },
                drawableId = R.drawable.reload,
                contentDescriptionId = R.string.reload_route
            )
        }
        MapControlButtonSpacer()
        if (myLocationButtonEnabled) {
            MapControlButton(
                onClick = { clickHandler.onMyLocation() },
                drawableId = R.drawable.my_location,
                contentDescriptionId = R.string.my_location,
            )
        }
        MapControlButtonSpacer()
        MapControlButton(
            onClick = { clickHandler.onZoomIn() },
            drawableId = R.drawable.zoom_in,
            contentDescriptionId = R.string.zoom_in
        )
        MapControlButton(
            onClick = { clickHandler.onZoomOut() },
            drawableId = R.drawable.zoom_out,
            contentDescriptionId = R.string.zoom_out
        )
        if (routeAvailable) {
            MapControlButton(
                onClick = { clickHandler.onShowRoute() },
                drawableId = R.drawable.zoom_to_show_route,
                contentDescriptionId = R.string.show_route
            )
        }
        Spacer(Modifier.size(Dp(0f), Dp(LocalConfiguration.current.screenHeightDp * 0.1f)))
        MapControlButton(
            onClick = { clickHandler.onAbout() },
            drawableId = R.drawable.about,
            contentDescriptionId = R.string.about
        )
    }
}

@Composable
private fun MapControlButton(
    onClick: () -> Unit, drawableId: Int, contentDescriptionId: Int
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(dimensionResource(R.dimen.map_control_size))
            .background(colorResource(id = R.color.map_control_background))
            .padding(dimensionResource(R.dimen.default_padding))
    ) {
        Icon(
            painterResource(id = drawableId),
            contentDescription = stringResource(contentDescriptionId),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun MapControlButtonSpacer() {
    Spacer(modifier = Modifier.size(dimensionResource(R.dimen.map_control_gap)))
}


private fun makeMarkerDrawable(context: Context, resId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, resId)
    vectorDrawable!!.setBounds(
        0,
        0,
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight
    )
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    vectorDrawable.draw(Canvas(bitmap))
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Preview
@Composable
fun MapControlButtonsPreview() {
    val clickHandler = object : MapButtonsClickHandler {
        override fun onZoomIn() {}
        override fun onZoomOut() {}
        override fun onChooseRoute() {}
        override fun onMyLocation() {}
        override fun onShowRoute() {}
        override fun onReloadRoute() {}
        override fun onAbout() {}
    }
    MapControlButtons(
        routeAvailable = true,
        clickHandler = clickHandler,
        myLocationButtonEnabled = true,
        alignment = UiAlignment.Right,
    )
}
