package com.example.localists

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.localists.databinding.FragmentGeofenceMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class GeofenceMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentGeofenceMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private var geofenceCircle: Circle? = null // Track the main (inner) circle
    private var geofenceGlow: Circle? = null // NEW: Track the outer glow ring

    // NEW: Permission request code
    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGeofenceMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // NEW: Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // NEW: Get the SupportMapFragment and set callback
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // NEW: Set up long-press listener for adding geofence
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            mMap.setOnMapLongClickListener { latLng ->
                addGeofenceCircle(latLng)
            }
        }
    }

    // NEW: Callback when map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // UPDATED: Initial disable of zoom controls (reinforced later if needed)
        mMap.uiSettings.isZoomControlsEnabled = false

        // NEW: Enable location on map if permission granted
        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }
    }

    // NEW: Check for location permission
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // NEW: Request location permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // NEW: Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission needed for map features", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // NEW: Enable my location button and fetch current position
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap.isMyLocationEnabled = true

        // UPDATED: Reinforce zoom disable after location UI loads (fixes timing quirks)
        mMap.uiSettings.isZoomControlsEnabled = false

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f)) // Zoom to 15 for street-level view
                // Optional: Add a marker at current location
                mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Current Location"))
            }
        }.addOnFailureListener { e ->
            Log.e("GeofenceMap", "Failed to get location", e)
            Toast.makeText(requireContext(), "Couldn't get location—check GPS", Toast.LENGTH_SHORT).show()
        }
    }

    // UPDATED: Add a modern geofence with inner fill and outer glow ring
    private fun addGeofenceCircle(latLng: LatLng) {
        // Remove previous circles if exist
        geofenceCircle?.remove()
        geofenceGlow?.remove()

        // Define theme color
        val themeBlue = Color.parseColor("#0072b1") // bice_blue from palette
        val faintBlue = 0x260072B1.toInt() // 15% opacity for subtle inner fill

        // NEW: Inner circle with faint fill
        geofenceCircle = mMap.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(10.0) // Standard 10m for testing
                .fillColor(faintBlue)
                .strokeColor(themeBlue)
                .strokeWidth(0f) // No inner stroke to let glow handle outline
        )

        // NEW: Outer glow ring (slightly larger, stroke-only for halo effect)
        geofenceGlow = mMap.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(12.0) // 2m larger for subtle expansion
                .fillColor(Color.TRANSPARENT) // No fill
                .strokeColor(themeBlue)
                .strokeWidth(5f) // Thicker for visibility
        )

        Toast.makeText(requireContext(), "Geofence set at ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()

        // TODO: Later, link this to a TaskItem (e.g., save lat/lng/radius to task)
    }
}