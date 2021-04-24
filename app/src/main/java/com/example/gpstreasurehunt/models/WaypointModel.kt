package com.example.gpstreasurehunt.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

/**
 *
 * WaypointModel.kt
 * Version 1.0
 * Last modified: 24/04/21
 * @author William Harvey
 *
 *
 * A model (object) class for the Waypoint model.
 * Class declaration also acts as the constructor.
 * Parcelable so object can be added to a bundle and parsed between fragments/activities.
 * @property idInput - The id of the model
 * @property latitudeInput - The latitude of the model
 * @property longitudeInput - The longitude of the model
 */
@Parcelize
class WaypointModel(
    var idInput: Int,
    var latitudeInput: Double,
    var longitudeInput: Double
) : Parcelable {

    private var idNum: Int? = idInput //ID of the object
    private var latitudeNum: Double? = latitudeInput //The latitude of the object
    private var longitudeNum: Double? = longitudeInput //The longitude of the object


    private var pointsArrayList = ArrayList<Pair<Double, Double>>() //An arraylist of all
    // the points in the treasure hunt

    /**
     * Generates a pair of doubles for the lat and long of a point in the treasure hunt.
     *
     * @param latInput
     * @param longInput
     * @return
     */
    private fun generatePair(latInput: Double, longInput: Double): Pair<Double, Double>{
        //Generates the point within a radius of 5m from the previous
        val pairLatitude = Random.nextDouble((latInput - 0.000005), (latInput + 0.000005))
        val pairLongitude = Random.nextDouble((longInput - 0.000005), (longInput + 0.000005)) * -1
        if((pairLatitude == latitudeNum && pairLongitude == longitudeNum) ||
            (pairLatitude == latInput && pairLongitude == longInput)){
            generatePair(latInput, longInput)
        }
        return Pair(pairLatitude, pairLongitude)
    }

    /**
     * Generates a random array of pairs and sets it to the model.
     *
     */
    fun generateRandomPointsArray() {
        var latitudeGen = latitudeNum!!
        var longitudeGen = longitudeNum!!
        pointsArrayList.add(Pair(latitudeNum!!, longitudeNum!!))
        for (i in 1..4) {
            //Generates a pair and adds it to the list.
            var addPair = generatePair(latitudeGen, longitudeGen)
            pointsArrayList.add(addPair)
            latitudeGen = addPair.first
            longitudeGen = addPair.second
        }
    }

    /**
     * Sets the pointsArrayList
     *
     * @param inputArray - The new pointsArrayList
     */
    fun setPointsArray(inputArray: ArrayList<Pair<Double, Double>>){
        this.pointsArrayList = inputArray
    }

    /**
     * Sets the latitude
     *
     * @param latitude - The new latitude
     */
    fun setLatitude(latitude: Double) {
        this.latitudeNum = latitude
    }

    /**
     * Sets the longitude
     *
     * @param longitude - The new longitude
     */
    fun setLongitude(longitude: Double) {
        this.longitudeNum = longitude
    }

    /**
     * Gets the idNum
     *
     * @return idNum
     */
    fun getId(): Int {
        return idNum!!.toInt()
    }

    /**
     * Gets the latitudeNum
     *
     * @return latitudeNum
     */
    fun getLatitude(): Double {
        return latitudeNum!!.toDouble()
    }

    /**
     * Gets the longitudeNum
     *
     * @return longitudeNum
     */
    fun getLongitude(): Double {
        return longitudeNum!!.toDouble()
    }

    /**
     * Gets the pointsArrayList
     *
     * @return pointsArrayList
     */
    fun getPairArray(): ArrayList<Pair<Double, Double>>{
        return pointsArrayList
    }


}