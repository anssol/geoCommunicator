package com.example.geocommunicator

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.geocommunicator.Constants.Companion.DEVICE_PERMISSION_REQUEST_CODE
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import android.provider.Settings
import android.widget.TextView
import com.example.geocommunicator.Constants.Companion.LOCATION_PERMISSION_REQUEST_CODE

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    // Clients and intents
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var locationServiceIntent: Intent
    private lateinit var activityServiceIntent: Intent

    // Functions
    private lateinit var functions: Functions

    private val TAG = "MainActivity"
    private val RTAG = "MainReceiver"

    // For displaying text on screen
    private lateinit var latUpdateTextView : TextView
    private lateinit var lngUpdateTextView : TextView

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                "Activity" -> {
                    // Get Activity data
                    val message = intent.getStringExtra("Message")
                    Log.d(RTAG, "Got message: $message")
                    displayNotification(message) }

                "Location" -> {
                    // Get extra data included in the Intent
                    val latitude = intent.getStringExtra("latitude")
                    val longitude = intent.getStringExtra("longitude")
                    val accuracy = intent.getStringExtra("accuracy")
                    val speed = intent.getStringExtra("speed")
                    val time = intent.getStringExtra("locationTime")
                    val altitude = intent.getStringExtra("altitude")

                    /* Update TextViews */
                    val strLatitude = "Latitude: $latitude"
                    latUpdateTextView.invalidate()
                    latUpdateTextView.setText(strLatitude)

                    val strLongitude = "Longitude: $longitude"
                    lngUpdateTextView.invalidate()
                    lngUpdateTextView.setText(strLongitude)

                    /* Logs */
                    Log.d(RTAG, "Got longitude: $longitude")
                    Log.d(RTAG, "Got latitude: $latitude")
                    Log.d(RTAG, "Got accuracy: $accuracy")
                    Log.d(RTAG, "Got speed: $speed")
                    Log.d(RTAG, "Got time: $time")
                    Log.d(RTAG, "Got latitude: $altitude")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ask user to enable GPS if it's disabled.
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        // Put permissions here
        getDevicePermission()
        getLocationPermission()

        val deviceID = getDeviceID()
        Log.d(TAG, "Got ID: $deviceID")

        /* TextView parameters */
        latUpdateTextView = findViewById(R.id.latTextView)
        lngUpdateTextView = findViewById(R.id.lngTextView)

        locationServiceIntent = Intent(this, LocationUpdateService::class.java)
        locationServiceIntent.putExtra("deviceID", deviceID)

        /**
        ------------------------------------------------------------------
        Not allowed to run services in background on Android API > 26
        ------------------------------------------------------------------
        Source: https://stackoverflow.com/questions/46445265
        Instead: Replace 'Service' with 'JobService/JobIntentService'
        Alternative method (new). WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager/
        ----------------------------------------------------------------
         **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent)
        } else {
            startService(locationServiceIntent)
        }

        // Local Broadcast manager
        val intentFilter = IntentFilter()
        intentFilter.addAction("Activity")
        intentFilter.addAction("Location")
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter)

        // Initialize Intent client
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(ActivityRecognition.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        mGoogleApiClient.connect()

    }

    @SuppressLint("HardwareIds")
    private fun getDeviceID() : String {

        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, android.Manifest.permission.READ_PHONE_STATE)) {
            } else { ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.READ_PHONE_STATE), DEVICE_PERMISSION_REQUEST_CODE) } }
        val telephonyManager: TelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.getDeviceId()
    }

    override fun onConnected(p0: Bundle?) {
        activityServiceIntent = Intent(this, ActivityUpdateService::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getService(this, 0, activityServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 3000, pendingIntent)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        stopService(locationServiceIntent)
    }

    fun displayNotification(strMessage : String) {
        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("User Activity")
            .setContentText(strMessage)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setPriority(Notification.PRIORITY_HIGH)
        mBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(strMessage))

        val mNotificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // For Android version > Oreo (8.0), set channel ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelID = "0"
            val channel = NotificationChannel(channelID, "Channel 0", NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelID)
        }

        mNotificationManager.notify(0, mBuilder.build())
    }

    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11
                )
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun getDevicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return
            } else {
                Log.d(TAG, "Requesting Device Permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                    DEVICE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun getLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                return
            } else {
                Log.d(TAG, "Requesting Location Permission")
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ), LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: called")
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults) {
                        if (i != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult: Location Permission Failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: Location Permission Granted")
                }
            DEVICE_PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults) {
                        if (i != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult: Device Permission Failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: Device Permission Granted")
                }
        }
    }

}