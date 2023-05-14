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

        mapViewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        mapViewModel.inProgress.observe(this) {
            binding.inProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
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

        binding.zoomIn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.zoomOut.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomOut())
        }
    }

    private fun getMapFragment(): SupportMapFragment = supportFragmentManager
        .findFragmentById(R.id.map) as SupportMapFragment

    override fun onStart() {
        super.onStart()
        mapViewModel.start()
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

        map.uiSettings.isMyLocationButtonEnabled = false

        mapViewModel.initialCameraPos.observe(this) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, it.zoom))
        }

        map.setOnCameraMoveListener {
            val target = map.cameraPosition.target
            mapViewModel.onCameraMove(
                MapCameraPosition(
                    LatLng(target.latitude, target.longitude), map.cameraPosition.zoom
                )
            )
        }

        mapViewModel.situationImage.observe(this) {
            onSituationReady(it)
        }

        if (isLocationPermissionGranted()) {
            binding.myLocation.isEnabled = true
        }
    }

    private fun onSituationReady(situation: SituationImage?) {
        clearMarkers()
        if (situation != null) {
            placeMarkers(situation)
            showSituation()

            binding.routeNumber.visibility = View.VISIBLE
            binding.routeNumber.text =
                getString(R.string.title_format).format(situation.routeNumber)

            // TODO: animate
            binding.refresh.visibility = View.VISIBLE

        } else {
            binding.routeNumber.visibility = View.INVISIBLE
            binding.refresh.visibility = View.GONE
        }
    }

    private fun showSituation() {
        if (markers.isNotEmpty()) {
            val markerBounds = LatLngBounds.builder().apply {
                markers.forEach {
                    include(it.position)
                }
            }.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(markerBounds, 100))
        }
    }

    private fun placeMarkers(situation: SituationImage) {
        situation.markers.forEach {
            val marker = when (it.type) {
                SituationImage.Marker.Type.Bus -> addBusMarker(it)
                SituationImage.Marker.Type.Stop -> addStopMarker(it)
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

    private fun addBusMarker(markerModel: SituationImage.Marker): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    markerModel.location.lat,
                    markerModel.location.lon
                )
            )
            .title("Bus #306")
        val iconResId =
            if (markerModel.direction == Direction.Forward) R.drawable.red_arrow else R.drawable.blue_arrow
        markerOptions.icon(makeMarkerDrawable(iconResId))
        if (markerModel.heading != null) {
            markerOptions.rotation(markerModel.heading)
        }
        return map.addMarker(markerOptions)

    }

    private fun addStopMarker(markerModel: SituationImage.Marker): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    markerModel.location.lat,
                    markerModel.location.lon
                )
            )
        val iconResId =
            if (markerModel.direction == Direction.Forward) R.drawable.red_stop else R.drawable.blue_stop
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