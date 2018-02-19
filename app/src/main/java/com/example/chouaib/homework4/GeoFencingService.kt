package com.example.chouaib.homework4

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.BaseColumns
import android.util.Log

// source of the bellow code: https://stackoverflow.com/questions/28535703/best-way-to-get-user-gps-location-in-background-in-android


class GeoFencingService : Service() {

    private var mLocationManager: LocationManager? = null
    private lateinit var dbHelper: DatabaseManipulator.GeoMessagesDbHelper

    private val DISTANCE_ACCURACY = getString(R.string.distance_accurcy).toFloat() // accuracy of the distance in meters

    var mLocationListeners = arrayOf(LocationListener(LocationManager.GPS_PROVIDER), LocationListener(LocationManager.NETWORK_PROVIDER))

    inner class LocationListener(provider: String) : android.location.LocationListener {
        internal var mLastLocation: Location

        init {
            Log.e(TAG, "LocationListener $provider")
            mLastLocation = Location(provider)
        }

        override fun onLocationChanged(location: Location) {
            val coordinates = "${location.latitude}, ${location.longitude}"

            Log.e(TAG, "onLocationChanged: $coordinates")
            retrieveGeoNotes(location)
            mLastLocation.set(location)
        }

        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "onProviderDisabled: " + provider)
        }

        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "onProviderEnabled: " + provider)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.e(TAG, "onStatusChanged: " + provider)
        }
    }

    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        initializeLocationManager()

        dbHelper = DatabaseManipulator.GeoMessagesDbHelper(this)

        try {
            mLocationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL.toLong(), LOCATION_DISTANCE,
                    mLocationListeners[1])
        } catch (ex: java.lang.SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, " + ex.message)
        }

        try {
            mLocationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL.toLong(), LOCATION_DISTANCE,
                    mLocationListeners[0])
        } catch (ex: java.lang.SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "gps provider does not exist " + ex.message)
        }

    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        super.onDestroy()
        if (mLocationManager != null) {
            for (i in mLocationListeners.indices) {
                try {
                    mLocationManager!!.removeUpdates(mLocationListeners[i])
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex)
                }

            }
        }
    }

    private fun retrieveGeoNotes(currentlocation: Location) {


        val db = dbHelper.readableDatabase

        val geoMessagesEntry = DatabaseManipulator.GeoMessagesContract.GeoMessageEntry

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(BaseColumns._ID,
                geoMessagesEntry.COLUMN_NAME_TITLE,
                geoMessagesEntry.COLUMN_NAME_MESSAGE,
                geoMessagesEntry.COLUMN_NAME_LAT,
                geoMessagesEntry.COLUMN_NAME_LON)

        val cursor = db.query(
                geoMessagesEntry.TABLE_NAME,   // The table to query
                projection,             // The columns to return
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        )

        with(cursor) {
            while(moveToNext()){
                val lat = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_LAT))
                val lon = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_LON))
                val savedLocation = Location("point A")
                savedLocation.latitude = lat
                savedLocation.longitude = lon
                val distance = currentlocation.distanceTo(savedLocation)
                Log.e(TAG, "#### COMPUTED SISTANDE  ####    $distance ")
                if (distance <= DISTANCE_ACCURACY) {
                    val title = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_TITLE))
                    val message = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_MESSAGE))
                    // send notification
                }
            }
        }


    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager")
        if (mLocationManager == null) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    companion object {
        private val TAG = "GEO FENCING SERVICE"
        private val LOCATION_INTERVAL = 3000
        private val LOCATION_DISTANCE = 0.0F
    }
}