package com.example.gpstreasurehunt.models

class WaypointModel(idInput: Int, latitudeInput: Long, longitudeInput: Long, usesInput: Int) {

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