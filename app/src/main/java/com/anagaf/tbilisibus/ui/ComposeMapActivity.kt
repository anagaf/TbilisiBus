package com.anagaf.tbilisibus.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.Direction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "ComposeMapActivity"

@AndroidEntryPoint
class ComposeMapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Log.d(TAG, "Recomposition")

            val uiState by viewModel.uiState.collectAsState()

            var isMapReady by remember { mutableStateOf(false) }

            val cameraPositionState = rememberCameraPositionState {
                position = uiState.cameraPosition
            }

            LaunchedEffect(uiState.cameraPosition) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(uiState.cameraPosition),
                    1_000
                )
            }

            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    viewModel.onCameraMove(cameraPositionState.position)
                }
            }

            LaunchedEffect(uiState.cameraBounds) {
                if (uiState.cameraBounds != null) {
                    Log.d(TAG, "Animating camera bounds: ${uiState.cameraBounds}")
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(uiState.cameraBounds!!, 100),
                        1_000
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMapView(
                    markers = uiState.routeMarkers,
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapReady = true
                        viewModel.onMapReady()
                    },
                )
            }

            if (isMapReady) {
                MapControlButtons(
                    cameraPositionState = cameraPositionState,
                    routeAvailable = uiState.routeMarkers.isNotEmpty(),
                    onChooseRouteButtonClicked = {
                        viewModel.onChooseRouteButtonClicked()
                    },
                    onMyLocationButtonClicked = {
                        viewModel.onMyLocationButtonClicked()
                    },
                    onShowRouteButtonClicked = {
                        viewModel.zoomToShowRoute()
                    },
                    onReloadRouteButtonClicked = {
                        viewModel.onReloadRouteButtonClicked()
                    }
                )

                if (uiState.routNumberDialogRequired) {
                    RouteNumberDialog(onConfirmed = { routeNumber ->
                        viewModel.onRouteNumberChangeConfirmed(routeNumber)
                    }, onDismissed = {
                        viewModel.onRouteNumberChangeDismissed()
                    })
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

            if (uiState.routeNumber != null) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.default_padding))
                ) {
                    Text(
                        text = getString(R.string.title_format).format(uiState.routeNumber),
                        fontSize = 24.sp,
                        color = colorResource(id = R.color.map_control),
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleMapView(
    markers: List<MapUiState.Marker>,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    onMapLoaded: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
    ) {
        Log.d(TAG, "Map recomposition")

        val rememberedMarkers = remember(markers) {
            markers
        }

        Markers(markers = rememberedMarkers)
    }

}

@Composable
fun Markers(markers: List<MapUiState.Marker>) {
    Log.d(TAG, "Markers recomposition")
    for (marker in markers) {
        when (marker.type) {
            MapUiState.Marker.Type.Bus -> BusMarker(marker)
            MapUiState.Marker.Type.Stop -> StopMarker(marker)
        }
    }
}


@Composable
private fun BusMarker(marker: MapUiState.Marker) {
    val iconId = when (marker.direction) {
        Direction.Forward -> R.drawable.red_arrow
        Direction.Backward -> R.drawable.blue_arrow
    }
    Marker(
        state = MarkerState(position = marker.location),
        icon = makeMarkerDrawable(LocalContext.current, iconId)
    )
}

@Composable
fun StopMarker(marker: MapUiState.Marker) {
    val context = LocalContext.current
    val forwardIcon = remember(context) {
        makeMarkerDrawable(context, R.drawable.red_stop)
    }
    val backwardIcon = remember(context) {
        makeMarkerDrawable(context, R.drawable.blue_stop)
    }
    Marker(
        state = MarkerState(position = marker.location),
        icon = when (marker.direction) {
            Direction.Forward -> forwardIcon
            Direction.Backward -> backwardIcon
        }
    )
}


@Composable
private fun MapControlButtons(
    cameraPositionState: CameraPositionState,
    routeAvailable: Boolean,
    onChooseRouteButtonClicked: () -> Unit = {},
    onMyLocationButtonClicked: () -> Unit = {},
    onShowRouteButtonClicked: () -> Unit = {},
    onReloadRouteButtonClicked: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.default_padding)),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        val coroutineScope = rememberCoroutineScope()
        MapControlButton(
            onClick = {
                onChooseRouteButtonClicked()
            }, drawableId = R.drawable.bus,
            contentDescriptionId = R.string.choose_route
        )
        if (routeAvailable) {
            MapControlButton(
                onClick = {
                    onReloadRouteButtonClicked()
                }, drawableId = R.drawable.reload,
                contentDescriptionId = R.string.reload_route
            )
        }
        MapControlButtonSpacer()
        MapControlButton(
            onClick = {
                onMyLocationButtonClicked()
            },
            drawableId = R.drawable.my_location,
            contentDescriptionId = R.string.my_location,
        )
        MapControlButtonSpacer()
        MapControlButton(
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                }
            }, drawableId = R.drawable.zoom_in,
            contentDescriptionId = R.string.zoom_in
        )
        MapControlButton(
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                }
            }, drawableId = R.drawable.zoom_out,
            contentDescriptionId = R.string.zoom_out
        )
        if (routeAvailable) {
            MapControlButton(
                onClick = {
                    coroutineScope.launch {
                        onShowRouteButtonClicked()
                    }
                }, drawableId = R.drawable.zoom_to_show_route,
                contentDescriptionId = R.string.show_route
            )
        }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteNumberDialog(
    onConfirmed: (number: Int) -> Unit,
    onDismissed: () -> Unit
) {
    var number by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        title = {
            Text(text = stringResource(R.string.choose_route))
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmed(number.toInt())
                },
                enabled = number.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismissed()
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            TextField(
                value = number,
                onValueChange = {
                    if (it.isEmpty() || (it.length <= 3 && it.toIntOrNull() != null)) {
                        number = it
                    }
                },
                label = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        onDismissRequest = {}
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
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

//@Preview
//@Composable
//fun GoogleMapViewPreview() {
//    GoogleMapView(Modifier.fillMaxSize())
//}