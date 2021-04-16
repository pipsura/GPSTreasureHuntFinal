package com.example.gpstreasurehunt

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.gpstreasurehunt.models.WaypointModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan
import java.lang.Math.toDegrees
import kotlin.math.sign
import kotlin.random.Random


class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback, WaypointFragment.OnInputListener {

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
    private var waypointId = 0

    private var isHuntActive = false
    private var currentModelId = 0

    private var cheatModel: WaypointModel = WaypointModel(0, 0.0, 0.0)
    private var currentModel: WaypointModel = WaypointModel(0, 0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Toolbar creation
        val mainToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarMain)
        setSupportActionBar(mainToolbar)

        //Map
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        requestNewLocationData()

        //Widgets
        val buryButton = findViewById<Button>(R.id.buryButton)
        val digButton = findViewById<Button>(R.id.foundButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val directionText = findViewById<TextView>(R.id.directionTextView)


        digButton.isInvisible = true

        cancelButton.setOnClickListener{
            isHuntActive = false
        }
        cancelButton.isInvisible = true

    }

    private fun getUserModel(): WaypointModel{
        val userModel = WaypointModel(
            0,
            userMarker.position.latitude,
            userMarker.position.longitude
        )
        return userModel
    }

    private fun setCheatButton(inputModel: WaypointModel){
        val cheatButton = findViewById<Button>(R.id.cheatButton)
        val modelArray = inputModel.getPairArray()
        cheatButton.setOnClickListener{
            cheatModel = WaypointModel(0, modelArray[currentModelId].first, modelArray[currentModelId].second)
            println( modelArray[currentModelId].first.toString() + modelArray[currentModelId].second.toString())
        }

    }

    private suspend fun setCheatButtonThread(inputModel: WaypointModel){
        withContext(Main){
            setCheatButton(inputModel)
        }
    }

    private suspend fun treasureSearch(userModel: WaypointModel, inputModel: WaypointModel){

        val distance = findDistance(userModel, currentModel)
        val digButton = findViewById<Button>(R.id.foundButton)

        val modelArray = inputModel.getPairArray()
        val modelArraySize = modelArray.size
        var buttonText: String = ""
        println(modelArraySize.toString() + " " + currentModelId.toString())
        if(currentModelId == modelArraySize -1){
            buttonText = resources.getString(R.string.digButton)
        } else {
            buttonText = resources.getString(R.string.foundmarker)
        }
        when {
            distance <= 5 -> {
                setFoundButtonTextOnThread(buttonText)
                setViewVisibility(digButton, true)
                setDigButtonOnClick(modelArray)
            }
            distance <= 10 -> {
                setViewVisibility(digButton, false)
                Toast.makeText(this, resources.getString(R.string.closeAlert),
                    Toast.LENGTH_LONG).show()
            }
            else -> {
                setViewVisibility(digButton, false)
            }
        }
    }

    private fun treasureHunt(inputModel: WaypointModel) {
        CoroutineScope(Main).launch {
            val result = startTreasureHunt(inputModel)
            println("debug: $result")
        }
    }

    private suspend fun startTreasureHunt(inputModel: WaypointModel): Boolean {

        val modelArray = inputModel.getPairArray()
        val modelArrayLast = modelArray.last()
        for(i in modelArray){
            println(i.toString())
        }
        currentModel = WaypointModel(
            currentModelId, modelArray[currentModelId].first, modelArray[currentModelId].second)


        isHuntActive = true
        setHuntStartVisibility()

        while(isHuntActive) {

            val userModel = getUserModel()
            setCheatButtonThread(inputModel)
            treasureSearch(cheatModel, inputModel)
            val direction = calculateDirection(cheatModel, currentModel)
            setTextViewOnThread(direction)

                //println("debug: $direction")
            //println("debug: loop is still active")


            delay(1000)
        }

        currentModelId = 0
        setHuntEndVisibility()

        println("debug: loop is no longer active")
        return true
    }

    private fun setDigButtonOnClick(modelArray: ArrayList<Pair<Double, Double>>){
        val digButton = findViewById<Button>(R.id.foundButton)
            digButton.setOnClickListener {
            if (currentModelId ==  modelArray.size -1) {

                    isHuntActive = false
                    Toast.makeText(this, resources.getString(R.string.foundTreasureAlert),
                        Toast.LENGTH_LONG).show()

            } else {

                    currentModelId += 1
                    Toast.makeText(this, resources.getString(R.string.foundMarkerAlert),
                        Toast.LENGTH_LONG).show()
                    currentModel = WaypointModel(
                        currentModelId, modelArray[currentModelId].first, modelArray[currentModelId].second
                    )
                }
        }
    }

    private suspend fun setViewVisibility(view: View, visible: Boolean){
        withContext(Main){
            val viewToChange = view
            viewToChange.isVisible = visible
        }
    }

    private suspend fun setHuntStartVisibility(){
        val buryButton = findViewById<Button>(R.id.buryButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val digButton = findViewById<Button>(R.id.foundButton)
        val directionTextView = findViewById<TextView>(R.id.directionTextView)
        setViewVisibility(buryButton, false)
        setViewVisibility(cancelButton, true)
        setViewVisibility(digButton, false)
        setViewVisibility(directionTextView, true)
    }

    private suspend fun setHuntEndVisibility(){
        val buryButton = findViewById<Button>(R.id.buryButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val digButton = findViewById<Button>(R.id.foundButton)
        val directionTextView = findViewById<TextView>(R.id.directionTextView)
        setViewVisibility(buryButton, true)
        setViewVisibility(cancelButton, false)
        setViewVisibility(digButton, false)
        setViewVisibility(directionTextView, false)
    }

    private fun setTextView(inputText: String){
        val directionText = findViewById<TextView>(R.id.directionTextView)
        directionText.text = inputText
    }

    private suspend fun setTextViewOnThread(input: String){
        withContext(Main) {
            setTextView(input)
        }
    }

    private fun setFoundButtonText(inputText: String){
        val button = findViewById<Button>(R.id.foundButton)
        button.text = inputText
    }

    private suspend fun setFoundButtonTextOnThread(input: String){
        withContext(Main) {
            setFoundButtonText(input)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        val userModel = getUserModel()

        val fm = supportFragmentManager
        val createFragment = WaypointFragment()
        val args = Bundle()
        val argsParam = "waypoint"
        val model = marker.getTag()
        if (model == "User" || model == null) {
            return false
        }
        /*
        if (findDistance(userModel, model as WaypointModel) > 15){
            return false
        }*/

        val parseModel = model as Parcelable
        args.putParcelable(argsParam, parseModel)
        createFragment.arguments = args


        createFragment.show(fm, "Waypoint")


        return false;
    }

    private fun findDistance(waypointOne: WaypointModel, waypointTwo: WaypointModel): Float {

        val locationOne: Location = Location("")
        locationOne.latitude = waypointOne.getLatitude()
        locationOne.longitude = waypointOne.getLongitude()

        val locationTwo: Location = Location("")
        locationTwo.latitude = waypointTwo.getLatitude()
        locationTwo.longitude = waypointTwo.getLongitude()

        val distance = locationOne.distanceTo(locationTwo)
        return distance
    }

    override fun sendInput(input: Boolean, model: WaypointModel) {
        val id = model.getId()
        Log.e(TAG, "got the input: $input +  $id")
        isHuntActive = false
        treasureHunt(model)
    }

    //waypointOne will be the current location, waypointTwo is the desitnation
    //returns a string of the direction (i.e. "meters west")
    private fun calculateDirection(waypointOne: WaypointModel, waypointTwo: WaypointModel): String {
        var degrees: Double
        val direction: String

        val finalLat = waypointTwo.getLatitude() - waypointOne.getLatitude()
        val finalLong = waypointTwo.getLongitude() - waypointOne.getLongitude()

        val firstDegrees = toDegrees(atan(finalLong / finalLat))
        degrees = firstDegrees

        if (sign(finalLat) == -1.0 && sign(finalLong) == -1.0) {
            degrees = 180 + degrees
        } else if (sign(finalLat) == 1.0 && sign(finalLong) == -1.0) {
            degrees = 360 + degrees
            degrees = Math.abs(degrees)
        } else if (sign(finalLat) == -1.0 && sign(finalLong) == 1.0) {
            degrees += 180
            degrees = Math.abs(degrees)
        }

        direction = when (degrees) {
            in 337.5..360.0 -> resources.getString(R.string.north)
            in 0.0..22.5 -> resources.getString(R.string.north)
            in 22.5..67.5 -> resources.getString(R.string.northeast)
            in 67.5..112.5 -> resources.getString(R.string.east)
            in 112.5..157.5 -> resources.getString(R.string.southeast)
            in 157.5..202.5 -> resources.getString(R.string.south)
            in 202.5..247.5 -> resources.getString(R.string.southwest)
            in 247.5..292.5 -> resources.getString(R.string.west)
            in 292.5..337.5 -> resources.getString(R.string.northwest)
            else -> return "direction error  + $degrees + $firstDegrees"
        }
        return direction
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
        mMap.setMinZoomPreference(minZoom)
        //mMap.setMaxZoomPreference(maxZoom)
        populateList()
        populateMap()

        mMap.setOnMarkerClickListener(this)

        for (i in 0..8) {
            val model = waypointArrayList.get(i)
            val j = i + 1
            val model2 = waypointArrayList.get(j)
            println(findDistance(model, model2))
        }
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
            userMarker.setTag("User")
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
        for (i in 1..10) {
            val latitude: Double = Random.nextDouble(51.60, 51.62)
            val longitude: Double = Random.nextDouble(3.860, 3.880) * -1
            val model = WaypointModel(i, latitude, longitude)
            model.generateRandomPointsArray()
            waypointId.inc()
            waypointArrayList.add(model)
        }
    }

    private fun populateMap() {
        for (i in 0..9) {
            val model = waypointArrayList.get(i)
            val modelLocation = LatLng(model.getLatitude(), model.getLongitude())
            val waypoint = mMap.addMarker(
                MarkerOptions().position(modelLocation)
                    .title(model.getId().toString())
            )
            waypoint.setTag(model)
        }
    }


}