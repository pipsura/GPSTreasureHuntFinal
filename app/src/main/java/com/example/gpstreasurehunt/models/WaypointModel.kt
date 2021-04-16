package com.example.gpstreasurehunt.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

@Parcelize
class WaypointModel(
    var idInput: Int,
    var latitudeInput: Double,
    var longitudeInput: Double
) : Parcelable {

    private var idNum: Int? = idInput
    private var latitudeNum: Double? = latitudeInput
    private var longitudeNum: Double? = longitudeInput
    private var usesNum: Int? = 3


    private var pointsArrayList = ArrayList<Pair<Double, Double>>()

    private fun generatePair(latInput: Double, longInput: Double): Pair<Double, Double>{
        var pairLatitude = Random.nextDouble((latInput - 0.000005), (latInput + 0.000005))
        var pairLongitude = Random.nextDouble((longInput - 0.000005), (longInput + 0.000005)) * -1
        if((pairLatitude == latitudeNum && pairLongitude == longitudeNum) ||
            (pairLatitude == latInput && pairLongitude == longInput)){
            generatePair(latInput, longInput)
        }
        return Pair(pairLatitude, pairLongitude)
    }

    fun generateRandomPointsArray(){
        var latitudeGen = latitudeNum!!
        var longitudeGen = longitudeNum!!
        pointsArrayList.add(Pair(latitudeNum!!, longitudeNum!!))
        for (i in 1..4){
            var addPair = generatePair(latitudeGen, longitudeGen)
            pointsArrayList.add(addPair)
            latitudeGen = addPair.first
            longitudeGen = addPair.second
        }
    }


    fun incrementUses() {
        this.usesNum?.inc()
    }

    fun decrementUses() {
        this.usesNum?.dec()
    }

    fun setPointsArray(inputArray: ArrayList<Pair<Double, Double>>){
        this.pointsArrayList = inputArray
    }

    fun setLatitude(latitude: Double) {
        this.latitudeNum = latitude
    }

    fun setLongitude(longitude: Double) {
        this.longitudeNum = longitude
    }

    fun setUses(uses: Int) {
        this.usesNum = uses
    }

    fun getId(): Int {
        return idNum!!.toInt()
    }

    fun getLatitude(): Double {
        return latitudeNum!!.toDouble()
    }

    fun getLongitude(): Double {
        return longitudeNum!!.toDouble()
    }

    fun getUses(): Int {
        return usesNum!!.toInt()
    }

    fun getPairArray(): ArrayList<Pair<Double, Double>>{
        return pointsArrayList
    }


}