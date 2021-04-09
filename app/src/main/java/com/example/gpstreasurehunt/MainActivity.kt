package com.example.gpstreasurehunt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import android.content.ContentValues.TAG
import android.os.Parcelable
import com.example.gpstreasurehunt.models.WaypointModel
import kotlin.random.Random
import kotlin.random.nextLong

class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var intervalVal = 3000;
    private var fastestIntervalVal = 1000;
    private var minZoom = 15.0f
    private var maxZoom = 25.0f
    private val LOCATION_PERMISSION_REQ_CODE = 1000
    private lateinit var userMarker: Marker

    private val defaultLocation = LatLng(0.0, 0.0)

    private var waypointArrayList = ArrayList<WaypointModel>()
    private var waypointId = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Toolbar creation
        val mainToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarMain)
        setSupportActionBar(mainToolbar)

        //Map
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestNewLocationData()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate((R.menu.main_menu_layout), menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            var success = false
            success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Cant find style, Error", e)
        }

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))
        //mMap.setMinZoomPreference(minZoom)
        //mMap.setMaxZoomPreference(maxZoom)
        populateList()
        populateMap()

        mMap.setOnMarkerClickListener(this)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = intervalVal.toLong()
        mLocationRequest.fastestInterval = fastestIntervalVal.toLong()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE
            )
            return
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val lat = mLastLocation.latitude
            val long = mLastLocation.longitude

            val lastLoc = LatLng(lat, long)

            if (::userMarker.isInitialized) {
                userMarker.remove()
            }

            userMarker = mMap.addMarker(MarkerOptions().position(lastLoc).title("Current location"))
            mMap.animateCamera(CameraUpdateFactory.newLatLng(lastLoc))
        }
    }


    private fun getLastLocation() {
        if (isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE
                )
                return
            }
            mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                val location: Location? = task.result
                if (location == null) {
                    requestNewLocationData()
                } else {
                    val lat = location.latitude
                    val long = location.longitude

                    Log.i("LocLatLocation", "$lat and $long")

                    val lastLoc = LatLng(lat, long)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(lastLoc))
                }
            }
        }
    }

    private fun populateList() {
        for (i in 0..9) {
            var latitude: Double = Random.nextDouble(5.160, 5.162)
            var longitude: Double = Random.nextDouble(3.860, 3.880)*-1
            var model = WaypointModel(i, latitude, longitude, 3)
            waypointId.inc()
            waypointArrayList.add(model)
        }
    }

    private fun populateMap() {
        for (i in 0..9) {
            var model = waypointArrayList.get(i)
            val modelLocation = LatLng(model.getLatitude(), model.getLongitude())
            val waypoint = mMap.addMarker(
                MarkerOptions().position(modelLocation)
                    .title(model.getId().toString())
            )
            //val lel: WaypointModel = waypoint.getTag() as WaypointModel
            //val poop = lel.getLatitude()
        }
    }


    public override fun onMarkerClick(marker: Marker): Boolean {
        val fm = supportFragmentManager
        val createFragment = WaypointFragment()
        val args: Bundle? = null
        val argsParam = "waypoint"
        val model = marker.getTag()

        args?.putParcelable(argsParam, model as? Parcelable)
        createFragment.arguments = args


        createFragment.show(fm, "CreateAccountDialog")
        return false;
    }

}