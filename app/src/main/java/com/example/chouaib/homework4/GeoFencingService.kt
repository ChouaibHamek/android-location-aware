package com.example.chouaib.homework4

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.BaseColumns
import android.support.annotation.RequiresApi
import android.util.Log


class GeoFencingService : Service() {

    private var mLocationManager: LocationManager? = null
    private lateinit var dbHelper: DatabaseManipulator.GeoMessagesDbHelper
    private lateinit var mNM: NotificationManager
    private var notificationON = false
    private var launchedNotificationId = 1

    private val DISTANCE_ACCURACY = 50.0F // accuracy of the distance in meters

    var mLocationListeners = arrayOf(LocationListener(LocationManager.GPS_PROVIDER), LocationListener(LocationManager.NETWORK_PROVIDER))

    inner class LocationListener(provider: String) : android.location.LocationListener {
        internal var mLastLocation: Location

        init {
            Log.e(TAG, "LocationListener $provider")
            mLastLocation = Location(provider)
        }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
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
        mNM = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun retrieveGeoNotes(currentlocation: Location) {


        val db = dbHelper.readableDatabase

        val geoMessagesEntry = DatabaseManipulator.GeoMessagesContract.GeoMessageEntry

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(BaseColumns._ID,
                geoMessagesEntry.COLUMN_NAME_TITLE,
                geoMessagesEntry.COLUMN_NAME_MESSAGE,
                geoMessagesEntry.COLUMN_NAME_LAT,
                geoMessagesEntry.COLUMN_NAME_LON,
                geoMessagesEntry.COLUMN_NAME_DATE
                )

        val orderBy = "ROWID DESC"
        val limit = "1"

        val cursor = db.query(
                geoMessagesEntry.TABLE_NAME,   // The table to query
                projection,             // The columns to return
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                orderBy,               // The sort order
                limit
        )

        with(cursor) {
            while(moveToNext()){
                val id = getInt(getColumnIndexOrThrow(BaseColumns._ID))
                Log.d(TAG, "Cursor at $id")
                val lat = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_LAT))
                val lon = getDouble(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_LON))

                val savedLocation = Location("point A")
                savedLocation.latitude = lat
                savedLocation.longitude = lon

                val coordinates = "${savedLocation.latitude}, ${savedLocation.longitude}"
                Log.e(TAG, "onLocationChanged: $coordinates")

                val distance = currentlocation.distanceTo(savedLocation)
                Log.e(TAG, "#### COMPUTED DISTANDE  ####    $distance ")

                if (distance > DISTANCE_ACCURACY) {
                    // cancel notification
                    mNM.cancel(launchedNotificationId)
                    notificationON = !notificationON
                } else {
                    // launch-update notification
                    val title = getString(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_TITLE))
                    val message = getString(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_MESSAGE))
                    val creationDate = getString(getColumnIndexOrThrow(geoMessagesEntry.COLUMN_NAME_DATE))
                    if (notificationON){
                        updateNotification(launchedNotificationId, title, message, creationDate, distance.toInt().toString())
                    } else {
                        showNotification(launchedNotificationId, title, message, creationDate, distance.toInt().toString())
                        notificationON = !notificationON
                    }
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun showNotification(id: Int, title: String, message: String, creationDate: String, distance: String) {

        Log.e(TAG, "#### SHOWING NOTIFICATION  ####    $id ")

        // The PendingIntent to launch our activity if the user selects this notification
        val contentIntent = PendingIntent.getActivity(this, 0,
                Intent(this, R.layout.activity_main::class.java), 0)

        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Set the info for the views that show in the notification panel.
        val notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_icon_background)
                .setSound(notificationSound)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(message)
                .setSubText("set on $creationDate | {$distance}m away")
                .setContentIntent(contentIntent)
                .build()

        // Send the notification.
        mNM.notify(id, notification)

    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun updateNotification(id: Int, title: String, message: String, creationDate: String, distance: String) {

        Log.e(TAG, "#### SHOWING NOTIFICATION  ####    $id ")

        // The PendingIntent to launch our activity if the user selects this notification
        val contentIntent = PendingIntent.getActivity(this, 0,
                Intent(this, R.layout.activity_main::class.java), 0)

        // Set the info for the views that show in the notification panel.
        val notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_icon_background)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(message)
                .setSubText("set on $creationDate | ${distance}m away")
                .setContentIntent(contentIntent)
                .build()

        // Send the notification.
        mNM.notify(id, notification)

    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager")
        if (mLocationManager == null) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    companion object {
        private val TAG = "GEO FENCING SERVICE"
        private val LOCATION_INTERVAL = 1000
        private val LOCATION_DISTANCE = 0.0F
    }
}