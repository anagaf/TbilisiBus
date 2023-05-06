package com.anagaf.tbilisibus.ui

import android.Manifest
import android.R.attr.maxLength
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapBinding

    private val mapViewModel: MapViewModel by viewModels()

    private val markers = mutableListOf<Marker>()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocationMapControls()
            } else {
                // TODO: dialog
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        getMapFragment().getMapAsync(this)

        mapViewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show();
        }

        mapViewModel.inProgress.observe(this) {
            binding.inProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        binding.bus.setOnClickListener {
            showBusNumberDialog()
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mapViewModel.initialCameraPos.observe(this) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, it.zoom))
        }

        mMap.setOnCameraMoveListener {
            val target = mMap.cameraPosition.target
            mapViewModel.onCameraMove(
                MapCameraPosition(
                    LatLng(target.latitude, target.longitude), mMap.cameraPosition.zoom
                )
            )
        }

        mMap.uiSettings.isZoomControlsEnabled = true

        if (isLocationPermissionGranted()) {
            enableMyLocationMapControls()

            mapViewModel.markers.observe(this) { markerDescriptions ->
                markers.forEach {
                    it.remove()
                }
                markers.clear()

                // Add markers for each location
                markerDescriptions.forEach { markerDescription ->
                    val marker = when (markerDescription.type) {
                        MarkerType.Bus -> addBusMarker(markerDescription)
                        MarkerType.Stop -> addStopMarker(markerDescription)
                    }
                    if (marker != null) {
                        markers.add(marker)
                    }
                }
            }
        }
    }

    private fun addBusMarker(markerDescription: MarkerDescription): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    markerDescription.location.lat,
                    markerDescription.location.lon
                )
            )
            .title("Bus #306")
        val iconResId =
            if (markerDescription.direction == Direction.Forward) R.drawable.red_arrow else R.drawable.blue_arrow
        markerOptions.icon(makeMarkerDrawable(iconResId))
        if (markerDescription.heading != null) {
            markerOptions.rotation(markerDescription.heading)
        }
        return mMap.addMarker(markerOptions)

    }

    private fun addStopMarker(markerDescription: MarkerDescription): Marker? {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    markerDescription.location.lat,
                    markerDescription.location.lon
                )
            )
        val iconResId =
            if (markerDescription.direction == Direction.Forward) R.drawable.red_stop else R.drawable.blue_stop
        markerOptions.icon(makeMarkerDrawable(iconResId))
        return mMap.addMarker(markerOptions)

    }

    private fun isLocationPermissionGranted() =
        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)

    @SuppressLint("MissingPermission")
    private fun enableMyLocationMapControls() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }

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
                mapViewModel.onBusNumberEntered(Integer.parseInt(number))
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
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show()

        numberEdit.requestFocus()
    }
}