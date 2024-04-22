package com.hw20.presentation.fragments.map

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hw20.App
import com.hw20.R
import com.hw20.databinding.FragmentMapBinding
import com.hw20.presentation.MainActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var latitudeMap = 0.0
    private var longitudeMap = 0.0
    private var zoomMap = 0.0f
    
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}


    private lateinit var mapView: MapView
    private lateinit var userLocationLayer: UserLocationLayer
    private var followUserLocation = false
    private var startLocation = Point(0.0, 0.0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.map

        if (isLocationPermissionGranted()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }

        binding.location.setOnClickListener {
            followUserLocation = true
            if (isLocationServiceEnabled()) {
                cameraUserPosition()
            } else {
                promptGpsActivation()
            }
            createNotification(latitudeMap, longitudeMap)
        }

        binding.plus.setOnClickListener { changeZoom(+ZOOM_STEP) }
        binding.minus.setOnClickListener { changeZoom(-ZOOM_STEP) }

        if (savedInstanceState != null) {
            latitudeMap = savedInstanceState.getDouble(LATITUDE)
            longitudeMap = savedInstanceState.getDouble(LONGITUDE)
            zoomMap = savedInstanceState.getFloat(ZOOM)
            mapView.mapWindow.map.move(
                CameraPosition(
                    Point(latitudeMap, longitudeMap),
                    zoomMap,
                    0f,
                    0f
                )
            )
        }
    }

    private fun changeZoom(value: Float) {
        with(mapView.mapWindow.map.cameraPosition) {
            latitudeMap = target.latitude
            longitudeMap = target.longitude
            zoomMap = zoom + value
            mapView.mapWindow.map.move(
                CameraPosition(target, zoomMap, azimuth, tilt),
                SMOOTH_ANIMATION,
                null
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble(LATITUDE, latitudeMap)
        outState.putDouble(LONGITUDE, longitudeMap)
        outState.putFloat(ZOOM, zoomMap)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun cameraUserPosition() {
        val cameraPosition = userLocationLayer.cameraPosition()
        if (cameraPosition != null && followUserLocation) {
            startLocation = cameraPosition.target
            latitudeMap = startLocation.latitude
            longitudeMap = startLocation.longitude
            zoomMap = 16f
            mapView.mapWindow.map.move(
                CameraPosition(startLocation, zoomMap, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }

    private fun promptGpsActivation() {
        if (!isLocationServiceEnabled()) {
            showPermissionDeniedDialog()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationServiceEnabled(): Boolean {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
//            if (!gpsEnabled) {
//                throw RuntimeException("Location providers are disabled")
//            }
        } catch (e: RuntimeException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//            if (!networkEnabled) {
//                throw RuntimeException("Location providers are disabled")
//            }
        } catch (e: RuntimeException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        return gpsEnabled && networkEnabled
    }

    private fun requestLocationPermission() {
        if (!isLocationPermissionGranted()) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun initializeMap() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(binding.map.mapWindow)
        userLocationLayer.isVisible = true
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.ask_turn_GPS))
            .setPositiveButton(
                R.string.open_location
            ) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createNotification(latitude: Double, longitude: Double) {
        val intent = Intent(requireContext(), MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notificationText = "Latitude: $latitude, Longitude: $longitude"
        val notification = NotificationCompat.Builder(requireContext(), App.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(requireContext()).notify(NOTIFICATION_ID, notification)
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val LATITUDE = "latitude"
        private const val LONGITUDE = "longitude"
        private const val ZOOM = "zoom"
        private const val ZOOM_STEP = 1f
        private val SMOOTH_ANIMATION = Animation(Animation.Type.SMOOTH, 0.4f)
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_ID = 1000
    }
}
