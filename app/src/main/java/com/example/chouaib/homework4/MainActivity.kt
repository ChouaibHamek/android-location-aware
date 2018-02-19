package com.example.chouaib.homework4

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.Manifest.permission
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 101

    private lateinit var locationManager: LocationManager
    private lateinit var lastLocation: Location
    private var locationGranted = false

    // Define a listener that responds to location updates
    var locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Called when a new location is found by the network location provider.
            setLocationInfo(location)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationGranted = true
                setLocationPermissionStatus()
                getLocation()
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }

    }

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


    @SuppressLint("MissingPermission")
    private fun getLocation(){
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        setLocationInfo(lastLocation)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0F, locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0F, locationListener)
    }

    private fun setLocationInfo(location: Location){
        if (locationGranted) {
            val coordinates = "${location.latitude}, ${location.longitude}"
            textViewCoordinatesDisplay.text = coordinates
        } else {
            textViewCoordinatesDisplay.text = "Please grant location access first"
        }
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

}
