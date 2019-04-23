package com.example.geocommunicator

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class LocationUpdateService : Service(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var deviceID : String

    private val TAG = "LocationUpdateService"

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground()
        }
        createLocationCallback()
        Log.d(TAG, "Service started")
        Log.d(TAG, "Received Device ID: " + intent.getStringExtra("deviceID"))
        deviceID = intent.getStringExtra("deviceID")
        buildGoogleApiClient()
        return Service.START_STICKY
    }

    private fun buildGoogleApiClient() {
        // Todo: Add Activity API
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(TAG, "Creating location request")
        mLocationRequest = LocationRequest.create()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 500
        Log.d(TAG, "Checking location permission.")
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        Log.d(TAG, "Permission OK. Requesting Location Updates...")
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.i(TAG, "Google API Client suspended.")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e(TAG, "Failed to connect to Google API Client.")
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationCallback)

        // disconnect from google api client
        Log.d(TAG, "Disconnecting from Google API Client.")
        mGoogleApiClient.disconnect()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Removing Location Updates")
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationCallback)
    }

    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (locationResult!!.lastLocation == null) return
                val latLng = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                Log.d("Location", latLng.latitude.toString() + " , " + latLng.longitude)

                /* Extract additional GPS parameters */
                val lastLocation = locationResult.lastLocation
                val horizontalAccuracy = lastLocation.accuracy
                val altitude = lastLocation.altitude
                val gpsDateTime = Functions().epochToDate(lastLocation.time)
                val gpsDate = gpsDateTime.first
                val gpsTime = gpsDateTime.second

                Log.d(TAG, lastLocation.time.toString())

                /* Broadcast Location Parameters */
                val intent = Intent("Location")

                intent.putExtra("latitude", latLng.latitude.toString())
                intent.putExtra("longitude", latLng.longitude.toString())
                intent.putExtra("accuracy", horizontalAccuracy.toString())
                intent.putExtra("locationDate", gpsDate)
                intent.putExtra("locationTime", gpsTime)
                intent.putExtra("altitude", altitude.toString())
                intent.putExtra("deviceID", deviceID)

                LocalBroadcastManager.getInstance(this@LocationUpdateService).sendBroadcast(intent)
                }
            }
        }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        startForeground(1, NotificationCompat.Builder(this, "Channel_ID")
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_icon_background)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running in background")
                .setContentIntent(pendingIntent)
                .build())
    }
}

