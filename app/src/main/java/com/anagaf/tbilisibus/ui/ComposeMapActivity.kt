package com.anagaf.tbilisibus.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anagaf.tbilisibus.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "BasicMapActivity"

val singapore = LatLng(1.35, 103.87)
val singapore2 = LatLng(1.40, 103.77)
val singapore3 = LatLng(1.45, 103.77)
val defaultCameraPosition = CameraPosition.fromLatLngZoom(singapore, 11f)

@AndroidEntryPoint
class ComposeMapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMapView(
                    viewModel = viewModel,
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                )
            }
        }
    }
}

@Composable
fun GoogleMapView(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapLoaded: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
        onPOIClick = {
            Log.d(TAG, "POI clicked: ${it.name}")
        }
    ) {
        // draw on map
    }
    MapControlButtons(cameraPositionState = cameraPositionState)
}

@Composable
private fun MapControlButtons(cameraPositionState: CameraPositionState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        MapControlButton(
            onClick = {
                cameraPositionState.move(CameraUpdateFactory.zoomIn())
            }, drawableId = R.drawable.my_location,
            contentDescriptionId = R.string.zoom_in
        )
        MapControlButton(
            onClick = {
                cameraPositionState.move(CameraUpdateFactory.zoomIn())
            }, drawableId = R.drawable.zoom_in,
            contentDescriptionId = R.string.zoom_in
        )
        MapControlButton(
            onClick = {
                cameraPositionState.move(CameraUpdateFactory.zoomOut())
            }, drawableId = R.drawable.zoom_out,
            contentDescriptionId = R.string.zoom_out
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
            .size(48.dp)
            .background(colorResource(id = R.color.map_control_background))
            .padding(8.dp)
    ) {
        Icon(
            painterResource(id = drawableId),
            contentDescription = stringResource(contentDescriptionId),
            tint = Color.Unspecified
        )
    }
}

//@Preview
//@Composable
//fun GoogleMapViewPreview() {
//    GoogleMapView(Modifier.fillMaxSize())
//}
