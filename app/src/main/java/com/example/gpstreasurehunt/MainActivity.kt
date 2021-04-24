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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan
import java.lang.Math.toDegrees
import kotlin.math.sign
import kotlin.random.Random

/**
 * TODO
 *
 */
class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback, WaypointFragment.OnInputListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var intervalVal = 3000
    private var fastestIntervalVal = 1000
    private var minZoom = 15.0f
    private var maxZoom = 25.0f
    private val LOCATION_PERMISSION_REQ_CODE = 1000
    private lateinit var userMarker: Marker

    private val defaultLocation = LatLng(0.0, 0.0)

    private var waypointArrayList = ArrayList<WaypointModel>()

    private var isHuntActive = false
    private var isBuryActive = false
    private var wasCancelled = false
    private var currentModelId = 0

    private var currentModel: WaypointModel = WaypointModel(0, 0.0, 0.0)

    private var userBuryModel = WaypointModel(0, 0.0, 0.0)
    private var userBuryPairList = ArrayList<Pair<Double, Double>>()

    private var isMarkerButtonClicked = false
    private lateinit var nextMarker: Pair<Double, Double>

    private val maxMarkerDistance = 25
    private val minMarkerDistance = 5

    private val loopDelay: Long = 50

    /**
     * TODO
     *
     * @param savedInstanceState
     */
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
        val markerButton = findViewById<Button>(R.id.buryMarkerButton)
        val finaliseButton = findViewById<Button>(R.id.finishBuryButton)

        digButton.isInvisible = true
        cancelButton.isInvisible = true
        markerButton.isInvisible = true
        finaliseButton.isInvisible = true

        cancelButton.setOnClickListener {
            isHuntActive = false
            isBuryActive = false
        }

        buryButton.setOnClickListener {
            treasureBury()
        }

    }

    /**
     * Starts the treasure bury function on a separate thread to the main activity using coroutines.
     *
     */
    private fun treasureBury() {
        CoroutineScope(Main).launch {
            runTreasureBury()
        }
    }

    /**
     * Runs all the needed functions and operations for the burying treasure activity.
     *
     */
    private suspend fun runTreasureBury() {

        //Clear the list from the previous time bury was activated
        userBuryPairList.clear()
        //Get the current user
        userBuryModel = getUserModel()
        userBuryPairList.add(Pair(userBuryModel.getLatitude(), userBuryModel.getLongitude()))

        //Setting up
        startTreasureBury()

        //While the user hasnt cancelled the burying activity:
        while (isBuryActive) {
            if (isMarkerButtonClicked) { //Adding a new point to the list in the new treasure route when clicked.
                val lastPair: Pair<Double, Double> = userBuryPairList.last()

                val lastModel = WaypointModel(0, lastPair.first, lastPair.second)
                val nextMarkerAsModel = WaypointModel(0, nextMarker.first, nextMarker.second)

                //If the marker is a valid distance:
                val distanceBetween = findDistance(lastModel, nextMarkerAsModel)
                if (distanceBetween > minMarkerDistance && distanceBetween < maxMarkerDistance &&
                    (lastModel.getLatitude() != nextMarkerAsModel.getLatitude() &&
                            lastModel.getLongitude() != nextMarkerAsModel.getLongitude())
                ) {
                    userBuryPairList.add(nextMarker)
                    makeToast(resources.getString(R.string.addMarkerSuccess))
                } else {
                    makeToast(resources.getString(R.string.addMarkerError) + distanceBetween)
                }
                isMarkerButtonClicked = false
            }
            delay(loopDelay)
        }
        //When the treasure hunt is cancelled/finished, carry out the finishing tasks.
        endTreasureBury()

    }

    /**
     * Setting up the TreasureBury action.
     *
     */
    private fun startTreasureBury() {

        isBuryActive = true

        //Setting the visibility of relevant widgets.
        val markerButton = findViewById<Button>(R.id.buryMarkerButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val finaliseButton = findViewById<Button>(R.id.finishBuryButton)
        val buryButton = findViewById<Button>(R.id.buryButton)
        markerButton.isInvisible = false
        cancelButton.isInvisible = false
        finaliseButton.isInvisible = false
        buryButton.isInvisible = true

        //Setting the onClickListeners for the relevant buttons.
        markerButton.setOnClickListener {
            isMarkerButtonClicked = true
            val userModel = getUserModel()
            val lat = userModel.getLatitude()
            val long = userModel.getLongitude()
            nextMarker = Pair(lat, long)
        }
        finaliseButton.setOnClickListener {
            isBuryActive = false
        }

    }

    /**
     * Ending the TreasureBury action.
     *
     */
    private fun endTreasureBury() {

        isBuryActive = false

        //Setting the visibility of the widgets.
        val markerButton = findViewById<Button>(R.id.buryMarkerButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val finaliseButton = findViewById<Button>(R.id.finishBuryButton)
        val buryButton = findViewById<Button>(R.id.buryButton)
        markerButton.isInvisible = true
        cancelButton.isInvisible = true
        finaliseButton.isInvisible = true
        buryButton.isInvisible = false

        //If the finalise button was clicked
        if (!wasCancelled) {
            //If the route is long enough, add it to the map.
            if (userBuryPairList.size > 3) {
                val modelToAdd = userBuryModel
                val arrayToAdd = userBuryPairList
                modelToAdd.setPointsArray(arrayToAdd)
                waypointArrayList.add(modelToAdd)
                addModelMarker(modelToAdd)
            } else {
                makeToast(resources.getString(R.string.finaliseError))
            }
        }

    }

    /**
     * Runs the treasureHunt with a coroutine to run on a different thread.
     *
     * @param inputModel - The model which the user will find the treasure of.
     */
    private fun treasureHunt(inputModel: WaypointModel) {
        CoroutineScope(Main).launch {
            startTreasureHunt(inputModel)
        }
    }

    /**
     * Running the treasure hunt.
     *
     * @param inputModel - The model which the user is finding the treasure of.
     */
    private suspend fun startTreasureHunt(inputModel: WaypointModel) {

        val modelArray = inputModel.getPairArray()

        //Creates a currentModel object, which will be used to store the data of what the next
        //marker in the route is
        currentModel = WaypointModel(
            currentModelId, modelArray[currentModelId].first, modelArray[currentModelId].second
        )

        isHuntActive = true
        setHuntStartVisibility() //Setting visiblity of widgets

        //While the hunt is active, run the treasureSearch method and display the distance and
        //direction to the next point/
        while (isHuntActive) {
            val userModel = getUserModel()
            treasureSearch(userModel, inputModel)
            val distance = findDistance(userModel, currentModel)
            val direction = calculateDirection(userModel, currentModel)
            val textOutput = "$distance $direction"
            setTextViewOnThread(textOutput)
            delay(loopDelay)
        }

        currentModelId = 0
        setHuntEndVisibility()//Setting the visibility of the widgets.
    }

    /**
     * Function which determines if the user has found the next marker on the treasure hunt.
     *
     * @param userModel - The model of the user
     * @param inputModel - The current model which the user is finding
     */
    private suspend fun treasureSearch(userModel: WaypointModel, inputModel: WaypointModel) {

        val distance = findDistance(userModel, currentModel)
        val digButton = findViewById<Button>(R.id.foundButton)

        val modelArray = inputModel.getPairArray()
        val modelArraySize = modelArray.size
        var buttonText: String = ""
        //If the user is at the final point, set the text to the given string, otherwise set it to
        //a different string.
        if (currentModelId == modelArraySize - 1) {
            buttonText = resources.getString(R.string.digButton)
        } else {
            buttonText = resources.getString(R.string.foundmarker)
        }
        when {
            //Determines what should happen depending on the users distance from the next point.
            distance <= 5 -> { //If the user is on the point, they can go to next point/dig
                setFoundButtonTextOnThread(buttonText)
                setViewVisibility(digButton, true)
                setDigButtonOnClick(modelArray)
            }
            distance <= 10 -> { //If the user is close, tell them
                setViewVisibility(digButton, false)
                Toast.makeText(
                    this, resources.getString(R.string.closeAlert),
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                setViewVisibility(digButton, false)
            }
        }
    }

    /**
     * Sets what should happen when the dig button is clicked.
     *
     * @param modelArray - The array of location pairs which the user is going through.
     */
    private fun setDigButtonOnClick(modelArray: ArrayList<Pair<Double, Double>>) {
        val digButton = findViewById<Button>(R.id.foundButton)
        digButton.setOnClickListener {
            if (currentModelId == modelArray.size - 1) { //If the user is on the last point

                isHuntActive = false
                //Tell them they succeeded
                Toast.makeText(
                    this, resources.getString(R.string.foundTreasureAlert),
                    Toast.LENGTH_LONG
                ).show()

            } else {
                //Move onto the next model
                currentModelId += 1
                Toast.makeText(
                    this, resources.getString(R.string.foundMarkerAlert),
                    Toast.LENGTH_LONG
                ).show()
                currentModel = WaypointModel(
                    currentModelId,
                    modelArray[currentModelId].first,
                    modelArray[currentModelId].second
                )
            }
        }
    }

    /**
     * Sets the visiblity of all the widgets when the hunt is started.
     *
     */
    private suspend fun setHuntStartVisibility() {
        val buryButton = findViewById<Button>(R.id.buryButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val digButton = findViewById<Button>(R.id.foundButton)
        val directionTextView = findViewById<TextView>(R.id.directionTextView)
        setViewVisibility(buryButton, false)
        setViewVisibility(cancelButton, true)
        setViewVisibility(digButton, false)
        setViewVisibility(directionTextView, true)
    }

    /**
     * Sets the visibility of all the widgets when the hunt is ended.
     *
     */
    private suspend fun setHuntEndVisibility() {
        val buryButton = findViewById<Button>(R.id.buryButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val digButton = findViewById<Button>(R.id.foundButton)
        val directionTextView = findViewById<TextView>(R.id.directionTextView)
        setViewVisibility(buryButton, true)
        setViewVisibility(cancelButton, false)
        setViewVisibility(digButton, false)
        setViewVisibility(directionTextView, false)
    }

    /**
     * Sets the visibility of the view that is parsed in. Done using a coroutine so function can be
     * accessed from a thread.
     *
     * @param view - The view to have its visiblity changed
     * @param visible - A boolean of what the visiblity should be - true = visible.
     */
    private suspend fun setViewVisibility(view: View, visible: Boolean) {
        withContext(Main) {
            val viewToChange = view
            viewToChange.isVisible = visible
        }
    }

    /**
     * Setting the text of the text view.
     *
     * @param inputText - A string of the text to be set
     */
    private fun setTextView(inputText: String) {
        val directionText = findViewById<TextView>(R.id.directionTextView)
        directionText.text = inputText
    }

    /**
     * Calling the setTextView function on a coroutine so it can be accessed by different threads.
     *
     * @param input - A string of the text to be set.
     */
    private suspend fun setTextViewOnThread(input: String) {
        withContext(Main) {
            setTextView(input)
        }
    }

    /**
     * Setting the text of the FoundButton.
     *
     * @param inputText - A string of the text to be set
     */
    private fun setFoundButtonText(inputText: String) {
        val button = findViewById<Button>(R.id.foundButton)
        button.text = inputText
    }

    /**
     * Calling the setFoundButtonText function on a coroutine so it can be accessed by different threads.
     *
     * @param input - A string of the text to be set.
     */
    private suspend fun setFoundButtonTextOnThread(input: String) {
        withContext(Main) {
            setFoundButtonText(input)
        }
    }

    /**
     * Makes and shows a toast with the input string.
     *
     * @param The text to be displayed on the toast.
     */
    private fun makeToast(input: String){
        Toast.makeText(
            this, input,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Sets the onClickListener for markers.
     * When a waypoint is clicked, open a dialog fragment with the data of what the waypoint is.
     *
     * @param marker - The marker that was clicked
     * @return - False
     */
    override fun onMarkerClick(marker: Marker): Boolean {

        val userModel = getUserModel()

        //Creating the fragment
        val fm = supportFragmentManager
        val createFragment = WaypointFragment()
        val args = Bundle()
        val argsParam = "waypoint"
        val model = marker.tag

        //End function if there is an error
        if (model == "User" || model == null) {
            return false
        }

        //TODO uncomment
        //if (findDistance(userModel, model as WaypointModel) > maxMarkerDistance){
        //   return false
        //}

        //Adds to the arguments of creating the fragment the waypoint model, which needs to be
        //parcelised to send the object.
        val parseModel = model as Parcelable
        args.putParcelable(argsParam, parseModel)
        createFragment.arguments = args

        createFragment.show(fm, "Waypoint")

        return false
    }

    /**
     * Gets the result from the dialog fragment on a positive result and activates the treasure hunt.
     * Overrides the interface declared in the fragment.
     *
     * @param input - Outcome of the function
     * @param model - The model of the waypoint which was pressed initially
     */
    override fun sendInput(input: Boolean, model: WaypointModel) {
        val id = model.getId()
        Log.e(TAG, "got the input: $input +  $id") //Debug
        //Begins the treasure hunt
        isHuntActive = false
        treasureHunt(model)
    }

    /**
     * Creates a WaypointModel using the current location of the user
     *
     * @return a model of the User
     */
    private fun getUserModel(): WaypointModel {
        return WaypointModel(
            0,
            userMarker.position.latitude,
            userMarker.position.longitude
        )
    }

    /**
     * Calculates the distances between the two input models.
     *
     * @param waypointOne - The first WaypointModel in the comparison
     * @param waypointTwo - The second WaypointModel in the comparison
     * @return A float of the distance
     */
    private fun findDistance(waypointOne: WaypointModel, waypointTwo: WaypointModel): Float {

        //Converts the WaypointModel objects to Location objects for the function distanceTo()
        val locationOne: Location = Location("")
        locationOne.latitude = waypointOne.getLatitude()
        locationOne.longitude = waypointOne.getLongitude()

        val locationTwo: Location = Location("")
        locationTwo.latitude = waypointTwo.getLatitude()
        locationTwo.longitude = waypointTwo.getLongitude()

        return locationOne.distanceTo(locationTwo)
    }

    /**
     * Calculates the cardinal direction which the second point is away from the first point.
     * It first calculates the angle using trigonometry from which the second point is away from
     * north (0 degrees) then fits it into the correct direction.
     *
     * @param waypointOne - The base point (usually the user's location).
     * @param waypointTwo - The point which will have the direction to found.
     * @return A string of the direction (i.e. "meters west")
     */
    private fun calculateDirection(waypointOne: WaypointModel, waypointTwo: WaypointModel): String {
        var degrees: Double
        val direction: String

        //Calculating the distances between the points (sides on the triangle).
        val finalLat = waypointTwo.getLatitude() - waypointOne.getLatitude()
        val finalLong = waypointTwo.getLongitude() - waypointOne.getLongitude()

        //Calculating the degrees of the angle - atan returns radians so it is converted.
        val firstDegrees = toDegrees(atan(finalLong / finalLat))
        degrees = firstDegrees

        //Calculates which quadrant on the compass the second point would fall.
        if (sign(finalLat) == -1.0 && sign(finalLong) == -1.0) { //Degrees is positive - South/East
            degrees += 180
        } else if (sign(finalLat) == 1.0 && sign(finalLong) == -1.0) { //Degrees is negative - North/East
            degrees += 360
            degrees = Math.abs(degrees)
        } else if (sign(finalLat) == -1.0 && sign(finalLong) == 1.0) { // Degrees is negative - South/West
            degrees += 180
            degrees = Math.abs(degrees)
        }

        direction = when (degrees) { //Sets the return var to the correct string.
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

    /**
     * TODO for future versions
     * Creates an options menu bar at the top of the screen
     *
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate((R.menu.main_menu_layout), menu)

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets up the map fragment
     *
     * @param googleMap - The map fragment
     */
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

        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))
        mMap.setMinZoomPreference(minZoom)
        mMap.setMaxZoomPreference(maxZoom)

        //Populates the map with randomly generated markers
        populateList()
        populateMap()

        //Set the onClickListener for waypoints to onMarkerClick
        mMap.setOnMarkerClickListener(this)
    }

    /**
     * Gets the location of the device
     *
     */
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

    /**
     * Sets the location of the user marker
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val lat = mLastLocation.latitude
            val long = mLastLocation.longitude

            val lastLoc = LatLng(lat, long)

            //Removes the last user marker on the map
            if (::userMarker.isInitialized) {
                userMarker.remove()
            }

            //Adds a new marker of the user to the map and sets the camera (view position) to it
            userMarker = mMap.addMarker(MarkerOptions().position(lastLoc).title("Current location"))
            userMarker.tag = "User"
            mMap.animateCamera(CameraUpdateFactory.newLatLng(lastLoc))
        }
    }

    /**
     * Populates the arraylist with randomly generated WaypointModel objects.
     *
     */
    private fun populateList() {
        for (i in 0..9) {
            //Generating random latitudes and longitudes near to Bay Campus
            val latitude: Double = Random.nextDouble(51.60, 51.62)
            val longitude: Double = Random.nextDouble(3.860, 3.880) * -1
            val model = WaypointModel(i, latitude, longitude)
            model.generateRandomPointsArray()
            waypointArrayList.add(model) //Add model to list
        }
    }

    /**
     * Populates the map with markers of all the models in the waypoint list.
     *
     */
    private fun populateMap() {
        for (i in 0..9) { //Loop hardcoded for current implementation, in future will loop through whole list
            val model = waypointArrayList[i]
            addModelMarker(model)
        }
    }

    /**
     * Adds a model object as a marker to the app.
     *
     * @param modelToAdd - The model that will be added to the map
     */
    private fun addModelMarker(modelToAdd: WaypointModel) {
        val modelLocation = LatLng(modelToAdd.getLatitude(), modelToAdd.getLongitude())
        val waypoint = mMap.addMarker(
            MarkerOptions().position(modelLocation)
                .title("")
        )
        waypoint.tag = modelToAdd //Adds data to the waypoint off the actual model object
    }


}