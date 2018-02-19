package com.example.chouaib.homework4

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 101

    private lateinit var locationManager: LocationManager
    private lateinit var lastLocation: Location
    private var locationGranted = false
    private lateinit var dbHelper: DatabaseManipulator.GeoMessagesDbHelper


    // Define a listener that responds to location updates
    var locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Called when a new location is found by the network location provider.
            lastLocation = location
            setLocationInfo(location)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        dbHelper = DatabaseManipulator.GeoMessagesDbHelper(this)


        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationGranted = true
                setLocationPermissionStatus()
                getLocation()
        } else {
            textViewCoordinatesDisplay.text = "Please grant location access first"
            ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }

        if (locationGranted) {
            Log.e("IN MAIN", " LOCATION IS GRANTED, STARTING SCHEDULE GEO FENCING SERVICE")
            this.startService(Intent(this, GeoFencingService::class.java))
        } else {
            textViewCoordinatesDisplay.text = "Please grant location access first"
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    locationGranted = true
                    setLocationPermissionStatus()
                    getLocation()

                } else {
                    locationGranted = false
                    setLocationPermissionStatus()

                }
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        setLocationInfo(lastLocation)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0F, locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0F, locationListener)
    }

    private fun setLocationInfo(location: Location){
        val coordinates = "${location.latitude}, ${location.longitude}"
        textViewCoordinatesDisplay.text = coordinates
    }

    private fun setLocationPermissionStatus( ){

        val status = if (locationGranted) "GRANTED" else "NOT GRANTED"

        val messageStatus = "LOCATION ACCESS $status"
        textViewPermission.text = messageStatus
        val messageColor = if (status == "GRANTED")
            Color.parseColor("#008000") else
            Color.parseColor("#FF0000")
        textViewPermission.setTextColor(messageColor)
    }

    fun onClickSaveNote(v: View) {

        val geoMessagesEntry = DatabaseManipulator.GeoMessagesContract.GeoMessageEntry

        // Gets the data repository in write mode
        val db = dbHelper.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(geoMessagesEntry.COLUMN_NAME_TITLE, "${editTextTitle.text}")
            put(geoMessagesEntry.COLUMN_NAME_MESSAGE, "${editTextMessage.text}")
            put(geoMessagesEntry.COLUMN_NAME_LON, "${lastLocation.longitude}")
            put(geoMessagesEntry.COLUMN_NAME_LAT, "${lastLocation.latitude}")
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db?.insert(geoMessagesEntry.TABLE_NAME, null, values)

        textViewCoordinatesDisplay.text = newRowId.toString()
    }

}
