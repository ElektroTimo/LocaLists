package com.example.localists

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.localists.databinding.FragmentNewTaskSheetBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions  // UPDATED: Correct import with .model
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.File
import java.time.LocalTime
import java.util.*

class NewTaskSheet(var taskItem: TaskItem? = null) : BottomSheetDialogFragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentNewTaskSheetBinding
    private val taskViewModel: TaskViewModel by activityViewModels()
    private var dueTime: LocalTime? = null
    private var imagePath: String? = null
    private var photoFile: File? = null

    // NEW: Geofence vars (capture from long-press for task save)
    private var geofenceLat: Double? = null
    private var geofenceLng: Double? = null
    private var geofenceRadius: Float? = null

    // Map-related vars
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private var geofenceCircle: Circle? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            Log.d("NewTaskSheet", "Image captured")
            photoFile?.let { file ->
                imagePath = file.absolutePath
                Log.d("NewTaskSheet", "Image path: $imagePath")
                Log.d("NewTaskSheet", "Image file exists: ${file.exists()}, readable: ${file.canRead()}, size: ${file.length()} bytes")
                binding.taskImage.setImageURI(android.net.Uri.fromFile(file))
            } ?: Log.e("NewTaskSheet", "Photo file is null")
        } else {
            Log.e("NewTaskSheet", "Image capture failed")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (taskItem != null) {
            binding.taskTitle.text = "Edit Task"
            binding.name.setText(taskItem!!.name)
            binding.desc.setText(taskItem!!.desc)
            if (taskItem!!.dueTime != null) {
                dueTime = taskItem!!.dueTime
                updateTimeButtonText()
            }
            taskItem!!.imagePath?.let { path ->
                binding.taskImage.setImageURI(android.net.Uri.fromFile(File(path)))
                imagePath = path
            }
            // NEW: Load existing geofence if editing (draw circle from saved coords)
            taskItem!!.latitude?.let { lat ->
                taskItem!!.longitude?.let { lng ->
                    taskItem!!.radiusMeters?.let { radius ->
                        val savedLatLng = LatLng(lat, lng)
                        addGeofenceCircle(savedLatLng, radius.toDouble())
                    }
                }
            }
        } else {
            binding.taskTitle.text = "New Task"
        }

        binding.saveButton.setOnClickListener {
            saveAction()
        }
        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }
        binding.capturePhotoButton.setOnClickListener {
            captureImage()
        }

        // Init map setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Map ready callback
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // NEW: Apply dark/light mode style
        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val styleRes = if (isNightMode) R.raw.map_night else R.raw.map_day  // Assumes you add these JSON styles (see note below)
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), styleRes))

        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

        // Long-press to add geofence
        mMap.setOnMapLongClickListener { latLng ->
            addGeofenceCircle(latLng)
        }
    }

    // Permission helpers
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

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
                Toast.makeText(requireContext(), "Location permission needed for map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Current Location"))
            }
        }.addOnFailureListener { e ->
            Log.e("NewTaskSheet", "Failed to get location", e)
            Toast.makeText(requireContext(), "Couldn't get location—check GPS", Toast.LENGTH_SHORT).show()
        }
    }

    // UPDATED: Add circle on long-press, capture coords for save
    private fun addGeofenceCircle(latLng: LatLng, radius: Double = 100.0) {
        geofenceCircle?.remove()
        geofenceCircle = mMap.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(radius)
                .fillColor(0x4080CBFF.toInt())
                .strokeColor(Color.BLUE)
                .strokeWidth(2f)
        )
        geofenceLat = latLng.latitude
        geofenceLng = latLng.longitude
        geofenceRadius = radius.toFloat()
        Toast.makeText(requireContext(), "Geofence set at ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
        Log.d("NewTaskSheet", "Geofence captured: lat=$geofenceLat, lng=$geofenceLng, radius=$geofenceRadius")
    }

    private fun captureImage() {
        photoFile = createImageFile()
        photoFile?.let { file ->
            Log.d("NewTaskSheet", "Created file at: ${file.absolutePath}")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "com.example.localists.provider",
                file
            )
            takePicture.launch(photoUri)
        } ?: Log.e("NewTaskSheet", "Failed to create image file")
    }

    private fun createImageFile(): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            Log.d("NewTaskSheet", "Creating file: ${absolutePath}, writable: ${canWrite()}")
        }
    }

    private fun openTimePicker() {
        if (dueTime == null)
            dueTime = LocalTime.now()
        val listener = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            dueTime = LocalTime.of(selectedHour, selectedMinute)
            updateTimeButtonText()
        }
        val dialog = TimePickerDialog(requireActivity(), listener, dueTime!!.hour, dueTime!!.minute, true)
        dialog.setTitle("Reminder for when?")
        dialog.show()
    }

    private fun updateTimeButtonText() {
        binding.timePickerButton.text = String.format("%02d:%02d", dueTime!!.hour, dueTime!!.minute)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    // UPDATED: Assign geofence to task on save
    private fun saveAction() {
        val name = binding.name.text.toString()
        val desc = binding.desc.text.toString()

        if (taskItem == null) {
            val newTask = TaskItem(
                name = if (name.isNotEmpty()) name else "Enter Title Here",
                desc = desc,
                dueTime = dueTime,
                completedDate = null,
                id = UUID.randomUUID(),
                imagePath = imagePath,
                latitude = geofenceLat,  // NEW: Assign captured geofence
                longitude = geofenceLng,
                radiusMeters = geofenceRadius
            )
            taskViewModel.addTaskItem(newTask)
            if (imagePath != null) {
                taskViewModel.setImagePath(newTask.id, imagePath!!)
            }
            Log.d("NewTaskSheet", "New task added: $name with geofence: lat=${newTask.latitude}, lng=${newTask.longitude}, radius=${newTask.radiusMeters}")
        } else {
            taskViewModel.updateTaskItem(taskItem!!.id, name, desc, dueTime)
            // NEW: Update geofence if changed (for edit mode)
            geofenceLat?.let { lat ->
                taskItem!!.latitude = lat
                taskItem!!.longitude = geofenceLng
                taskItem!!.radiusMeters = geofenceRadius
            }
            if (imagePath != null) {
                taskViewModel.setImagePath(taskItem!!.id, imagePath!!)
            }
            Log.d("NewTaskSheet", "Task updated: $name with geofence: lat=${taskItem!!.latitude}, lng=${taskItem!!.longitude}, radius=${taskItem!!.radiusMeters}")
        }

        taskViewModel.saveTaskItems(requireContext())
        binding.name.setText("")
        binding.desc.setText("")
        binding.taskImage.setImageDrawable(null)
        dismiss()
    }
}