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
import android.os.AsyncTask
import android.os.BatteryManager
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
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geocommunicator.Constants.Companion.LOCATION_PERMISSION_REQUEST_CODE
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import com.loopj.android.http.*;
import cz.msebera.android.httpclient.NameValuePair
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.message.BasicNameValuePair
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import kotlin.collections.ArrayList

// Todo: Accelerometer data
// Todo: Battery, CPU
// Todo: Distance to particular place?
class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    // Clients and intents
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var locationServiceIntent: Intent? = null
    private var activityServiceIntent: Intent? = null

    // Functions
    private lateinit var functions: Functions

    private val TAG = "MainActivity"
    private val RTAG = "MainReceiver"
    private val BTAG = "BatteryReceiver"

    // For displaying text on screen
    private lateinit var latUpdateTextView : TextView
    private lateinit var lngUpdateTextView : TextView

    // Permissions
    private var isLocationPermissionGranted = false
    private var isDevicePermissionGranted = false

    // For HTTP Request
    private val url = "http://130.240.134.129:8080/se.ltu.ssr.webapp/rest/fiwareproxy/ngsi10/updateContext/"

    // Database reference
    //private lateinit var firebaseConstructor : FirebaseConstructor

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                "Activity" -> {
                    // Get Activity data
                    val message = intent.getStringExtra("Message")
                    Log.d(RTAG, "Got message: $message")
                    displayNotification(message)
                }

                "Location" -> {
                    // Get extra data included in the Intent
                    val latitude = intent.getStringExtra("latitude")
                    val longitude = intent.getStringExtra("longitude")
                    val accuracy = intent.getStringExtra("accuracy")
                    val speed = intent.getStringExtra("speed")
                    val time = intent.getStringExtra("locationTime")
                    val altitude = intent.getStringExtra("altitude")
                    val deviceID = intent.getStringExtra("deviceID")

                    // Create JSON object to be sent
                    val message = createJSON(
                                    deviceID, altitude.toString(), accuracy.toString(),
                                    latitude.toString(), longitude.toString(), time,
                                    speed.toString())
                    Log.d(TAG, message)

                    // Update database with sensor information
                    SendDeviceDetails().execute(url, message)

                    /* Update TextViews */
                    val strLatitude = "Latitude: $latitude"
                    latUpdateTextView.invalidate()
                    latUpdateTextView.setText(strLatitude)

                    val strLongitude = "Longitude: $longitude"
                    lngUpdateTextView.invalidate()
                    lngUpdateTextView.setText(strLongitude)

                    /* Round decimals to 2 places */
                    // Remember to set the Locale!

                    /*
                    val lat = String.format(Locale.US, "%.2f", latitude).toDouble()
                    val lng = String.format(Locale.US, "%.2f", longitude).toDouble()
                    val altitudeVal = String.format(Locale.US, "%.2f", altitude).toDouble()
                    val timeVal = String.format(Locale.US, "%.2f", altitude).toLong()

                    val horizontalAccuracy = String.format(Locale.US, "%.2f", accuracy).toFloat()
                    val speedVal = String.format(Locale.US, "%.2f", speed).toFloat()


                    Log.d(TAG, "Updating Firease with Location Information")
                    /* Update database with location information */
                    firebaseConstructor.updateUserInfo(User(deviceID = deviceID, latitude = lat,
                        longitude = lng, horizontalAccuracy = horizontalAccuracy, speed = speedVal,
                        sampleDateTime = epochToDate(timeVal), altitude = altitudeVal))
                    */

                    /* Logs */
                    Log.d(RTAG, "Got longitude: $longitude")
                    Log.d(RTAG, "Got latitude: $latitude")
                    Log.d(RTAG, "Got accuracy: $accuracy")
                    Log.d(RTAG, "Got speed: $speed")
                    Log.d(RTAG, "Got time: $time")
                    Log.d(RTAG, "Got altitude: $altitude")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Put permissions here
        // Ask user to enable GPS if it's disabled.
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        // If Marshmallow+, ask for permission
        getLocationPermission()
        getDevicePermission()

        // Initialize Intent client
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        mGoogleApiClient.connect()

        if (isDevicePermissionGranted && isLocationPermissionGranted) {
            initiateServices()
        } else {
            // If Marshmallow+, ask for permission
            getLocationPermission()
            getDevicePermission()
        }
    }

    private fun initiateServices() {
        val deviceID = getDeviceID()
        Log.d(TAG, "Got ID: $deviceID")
        //firebaseConstructor = FirebaseConstructor(deviceID)
        //Log.d(TAG, "Created database instance with ID: $deviceID")

        /* TextView parameters */
        latUpdateTextView = findViewById(R.id.latTextView)
        lngUpdateTextView = findViewById(R.id.lngTextView)

        locationServiceIntent = Intent(this, LocationUpdateService::class.java)
        locationServiceIntent!!.putExtra("deviceID", deviceID)

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

        // Add battery actions
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW)
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY)

        // Battery level
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter)
    }

    private fun createJSON(IMEI : String, altitude : String, accuracy : String,
                           latitude : String, longitude : String, sampleDateTime : String,
                           speed : String) : String {

        val item = JSONObject()
        item.put("type", "Gateway")
        item.put("isPattern", "false")
        item.put("id", "UppsalaUni_01")

        // Create attributes
        val imei = JSONObject()
        imei.put("name", "imei")
        imei.put("type", "string")
        imei.put("value", IMEI)

        val jAltitude = JSONObject()
        jAltitude.put("name", "altitude")
        jAltitude.put("type", "string")
        jAltitude.put("value", altitude)

        val jHorizontalAccuracy = JSONObject()
        jHorizontalAccuracy.put("name", "horizontalAccuracy")
        jHorizontalAccuracy.put("type", "string")
        jHorizontalAccuracy.put("value", accuracy)

        val jLatitude = JSONObject()
        jLatitude.put("name", "latitude")
        jLatitude.put("type", "string")
        jLatitude.put("value", latitude)

        val jLongitude = JSONObject()
        jLongitude.put("name", "longitude")
        jLongitude.put("type", "string")
        jLongitude.put("value", longitude)

        val jSampleDateTime = JSONObject()
        jSampleDateTime.put("name", "sampleDateTime")
        jSampleDateTime.put("type", "string")
        jSampleDateTime.put("value", sampleDateTime)

        val jSpeed = JSONObject()
        jSpeed.put("name", "speed")
        jSpeed.put("type", "string")
        jSpeed.put("value", speed)

        val attributes = JSONArray()
        attributes.put(imei)
        attributes.put(jAltitude)
        attributes.put(jHorizontalAccuracy)
        attributes.put(jLatitude)
        attributes.put(jLongitude)
        attributes.put(jSampleDateTime)
        attributes.put(jSpeed)

        // Add all attributes
        item.put("attributes", attributes)

        val elements = JSONArray()
        elements.put(item)
        val contextElements = JSONObject()
        contextElements.put("contextElements", elements)
        contextElements.put("updateAction", "APPEND")

        val auth = JSONObject()
        auth.put("username", Constants.USERNAME)
        auth.put("password", Constants.PASSWORD)
        contextElements.put("auth", auth)

        val message: String
        message = contextElements.toString()

        return message
    }


    private fun getDevicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isDevicePermissionGranted = true
                Log.d(TAG, "DevicePermission is granted")
                return
            } else {
                Log.d(TAG, "Requesting Device Permission")
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                    DEVICE_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            isDevicePermissionGranted = true
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceID() : String {
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, android.Manifest.permission.READ_PHONE_STATE)) {
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.READ_PHONE_STATE), DEVICE_PERMISSION_REQUEST_CODE)
            }
        }
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

    private fun getLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                isLocationPermissionGranted = true
                Log.d(TAG, "LocationPermission is granted")
                getDevicePermission()
                return
            } else {
                Log.d(TAG, "Requesting Location Permission")
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ), LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            isLocationPermissionGranted = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: called")
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults) {
                        Log.d(TAG, i.toString())
                        if (i != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult: Location Permission Failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: Location Permission Granted")
                    getDevicePermission()
                    isLocationPermissionGranted = true
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
                    getDeviceID()
                    isDevicePermissionGranted = true
                }
        }
    }
}