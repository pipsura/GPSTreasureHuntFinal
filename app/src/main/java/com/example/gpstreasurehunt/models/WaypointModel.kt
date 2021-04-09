package com.example.gpstreasurehunt.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class WaypointModel(
    var idInput: Int,
    var latitudeInput: Double,
    var longitudeInput: Double,
    var usesInput: Int
) : Parcelable {

    private var idNum: Int? = idInput
    private var latitudeNum: Double? = latitudeInput
    private var longitudeNum: Double? = longitudeInput
    private var usesNum: Int? = usesInput

    fun setLatitude(latitude: Double) {
        this.latitudeNum = latitude
    }

    fun setLongitude(longitude: Double) {
        this.longitudeNum = longitude
    }

    fun setUses(uses: Int) {
        this.usesNum = uses
    }

    fun incrementUses() {
        this.usesNum?.inc()
    }

    fun decrementUses() {
        this.usesNum?.dec()
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


}