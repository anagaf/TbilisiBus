package com.anagaf.tbilisibus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.anagaf.tbilisibus.R
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.databinding.ActivityMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapBinding

    private val mapViewModel: MapViewModel by viewModels()

    private val markers = mutableListOf<Marker>()

    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                map.isMyLocationEnabled = true
                binding.myLocation.isEnabled = true
            } else {
                // TODO: dialog
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getMapFragment().getMapAsync(this)

        mapViewModel.state.observe(this) {
            onUiStateChange(it)
        }

        binding.bus.setOnClickListener {
            showBusNumberDialog()
        }

        binding.refresh.setOnClickListener {
            mapViewModel.updateSituation()
        }

        binding.myLocation.isEnabled = isLocationPermissionGranted()
        binding.myLocation.setOnClickListener {
            moveCameraToMyPosition()
        }

        binding.zoomIn.isEnabled = false
        binding.zoomIn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.zoomOut.isEnabled = false
        binding.zoomOut.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.zoomToShowRoute.visibility = View.GONE
        binding.zoomToShowRoute.setOnClickListener {
            mapViewModel.zoomToShowRoute()
        }
    }

    private fun onUiStateChange(uiState: MapUiState) {
        var route: MapUiState.RouteUiState? = null
        var inProgress = false

        when (uiState) {

            is MapUiState.InProgress -> {
                inProgress = true
            }

            is MapUiState.Error -> {
                Toast.makeText(this, uiState.message, Toast.LENGTH_SHORT).show()
            }

            is MapUiState.CameraMoveRequired -> {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        uiState.cameraPosition.latLng,
                        uiState.cameraPosition.zoom
                    )
                )
            }

            is MapUiState.CameraShowBoundsRequired -> {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(uiState.bounds, 100))
            }

            is MapUiState.RouteAvailable -> {
                route = uiState.route
            }
        }

        binding.inProgress.visibility =
            if (inProgress) View.VISIBLE else View.INVISIBLE

        if (route != null) {
            updateRoute(route)
        }
    }

    private fun updateRoute(route: MapUiState.RouteUiState) {
        binding.routeNumber.visibility = View.VISIBLE
        binding.refresh.visibility = View.VISIBLE
        binding.routeNumber.text =
            getString(R.string.title_format).format(route.routeNumber)

        drawMarkers(route.markers)
    }

    private fun getMapFragment(): SupportMapFragment = supportFragmentManager
        .findFragmentById(R.id.map) as SupportMapFragment

    override fun onStart() {
        super.onStart()
        if (!isLocationPermissionGranted()) {
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.isMyLocationEnabled = isLocationPermissionGranted()
        binding.zoomIn.isEnabled = true
        binding.zoomOut.isEnabled = true

        map.uiSettings.isMyLocationButtonEnabled = false

        map.setOnCameraMoveListener {
            val target = map.cameraPosition.target
            mapViewModel.onCameraMove(
                CameraPosition(
                    LatLng(target.latitude, target.longitude), map.cameraPosition.zoom
                )
            )
        }

        if (isLocationPermissionGranted()) {
            binding.myLocation.isEnabled = true
        }

        mapViewModel.onMapReady()
    }

    private fun drawMarkers(markers: List<MapUiState.Marker>) {
        clearMarkers()
        placeMarkers(markers)

        // TODO: animate
        binding.refresh.visibility = View.VISIBLE
        binding.zoomToShowRoute.visibility = View.VISIBLE
    }

    private fun placeMarkers(uiStateMarkers: List<MapUiState.Marker>) {
        uiStateMarkers.forEach {
            val marker = when (it.type) {
                MapUiState.Marker.Type.Bus -> addBusMarker(it)
                MapUiState.Marker.Type.Stop -> addStopMarker(it)
            }
            if (marker != null) {
                markers.add(marker)
            }
        }
    }

    private fun clearMarkers() {
        markers.forEach {
            it.remove()
        }
        markers.clear()
    }

    private fun addBusMarker(uiStateMarker: MapUiState.Marker): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    uiStateMarker.location.lat,
                    uiStateMarker.location.lon
                )
            )
            .title("Bus #306")
        val iconResId =
            if (uiStateMarker.direction == Direction.Forward) R.drawable.red_arrow else R.drawable.blue_arrow
        markerOptions.icon(makeMarkerDrawable(iconResId))
        if (uiStateMarker.heading != null) {
            markerOptions.rotation(uiStateMarker.heading)
        }
        return map.addMarker(markerOptions)

    }

    private fun addStopMarker(uiStateMarker: MapUiState.Marker): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    uiStateMarker.location.lat,
                    uiStateMarker.location.lon
                )
            )
        val iconResId =
            if (uiStateMarker.direction == Direction.Forward) R.drawable.red_stop else R.drawable.blue_stop
        markerOptions.icon(makeMarkerDrawable(iconResId))
        return map.addMarker(markerOptions)

    }

    private fun isLocationPermissionGranted() =
        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)

    private fun makeMarkerDrawable(resId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(this, resId)
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

    private fun showBusNumberDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Bus Number")

        val numberEdit = EditText(this)
        numberEdit.inputType = InputType.TYPE_CLASS_NUMBER
        numberEdit.filters = arrayOf<InputFilter>(LengthFilter(3))
        builder.setView(numberEdit)

        builder.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            val number = numberEdit.text.toString()
            if (number.isNotEmpty()) {
                mapViewModel.updateSituation(Integer.parseInt(number))
            }
        }

        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()

        numberEdit.setOnEditorActionListener { _, actionId, _ ->
            Boolean
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }

        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()

        numberEdit.requestFocus()
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToMyPosition() {
        val locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(
                                location.latitude,
                                location.longitude
                            )
                        )
                    )
                }
            }
    }
}