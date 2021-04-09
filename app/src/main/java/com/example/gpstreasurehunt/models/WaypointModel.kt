package com.example.gpstreasurehunt.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class WaypointModel(var idInput: Int,var latitudeInput: Long,var longitudeInput: Long,var usesInput: Int):
    Parcelable {

    private var idNum: Int? = idInput
    private var latitudeNum: Long? = latitudeInput
    private var longitudeNum: Long? = longitudeInput
    private var usesNum: Int? = usesInput



    fun setLatitude(latitude: Long){
        this.latitudeNum = latitude
    }

    fun setLongitude(longitude: Long){
        this.longitudeNum = longitude
    }

    fun setUses(uses: Int){
        this.usesNum = uses
    }

    fun incrementUses(){
        this.usesNum?.inc()
    }

    fun decrementUses() {
        this.usesNum?.dec()
    }

    fun getId(): Int {
        return idNum!!.toInt()
    }

    fun getLatitude(): Long {
        return latitudeNum!!.toLong()
    }

    fun getLongitude(): Long {
        return longitudeNum!!.toLong()
    }

    fun getUses(): Int {
        return usesNum!!.toInt()
    }



}