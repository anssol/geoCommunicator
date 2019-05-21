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
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.geocommunicator.Constants.Companion.LOCATION_PERMISSION_REQUEST_CODE
import java.util.*
import android.icu.text.DecimalFormat
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    // Clients and intents
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var locationServiceIntent: Intent? = null
    private var activityServiceIntent: Intent? = null
    private var sensorServiceIntent: Intent? = null
    private var batteryServiceIntent: Intent? = null

    // Functions (add external file later)
    private lateinit var functions: Functions

    // Logging tags for debugging
    private val TAG = "MainActivity"
    private val RTAG = "MainReceiver"
    private val BTAG = "BatteryReceiver"
    private val LTAG = "LocationReceiver"

    // To display text on screen
    private lateinit var latUpdateTextView : TextView
    private lateinit var lngUpdateTextView : TextView
    private lateinit var accUpdateTextView : TextView
    private lateinit var lightUpdateTextView : TextView

    // Buttons
    private lateinit var startButton : Button
    private lateinit var stopButton : Button

    // Permissions
    private var isLocationPermissionGranted = false
    private var isDevicePermissionGranted = false

    // URL LTU database server
    private val url = "http://130.240.134.129:8080/se.ltu.ssr.webapp/rest/fiwareproxy/ngsi10/updateContext/"
    private lateinit var jsonMessage : String

    // Firebase reference
    private lateinit var firebaseConstructor : FirebaseConstructor
    private var user = User()

    private val mMessageReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                "Activity" -> {
                    /**
                     * The Activity Data is used to give notifications to the user,
                     * e.g. if Walking, Running, Biking, In Car, etc.
                     */
                    /* Get Activity data */
                    val message = intent.getStringExtra("Message")
                    Log.d(RTAG, "Got message: $message")

                    user = user.copy(activity = message)
                    firebaseConstructor.updateUserInfo(user)

                    /* Create JSON Object and update LTU database */
                    jsonMessage = Functions().createJSON(user)
                    Log.d(TAG, jsonMessage)

                    /* Display the detected activity as a notification to the user. */
                    displayNotification(message)
                }

                "Location" -> {
                    /* Get extra data included in the Intent */
                    var latitude = intent.getStringExtra("latitude")
                    var longitude = intent.getStringExtra("longitude")
                    var accuracy = intent.getStringExtra("accuracy")
                    var locationDate = intent.getStringExtra("locationDate")
                    var locationTime = intent.getStringExtra("locationTime")
                    var altitude = intent.getStringExtra("altitude")
                    var deviceID = intent.getStringExtra("deviceID")

                    /* Optional: Format number of decimal points according to needs */
                    val formatter = DecimalFormat.getInstance(Locale.US)
                    formatter.maximumFractionDigits = 2
                    altitude = formatter.format(altitude.toDouble())
                    accuracy = formatter.format(accuracy.toFloat())

                    /*
                    latitude = formatter.format(latitude.toDouble())
                    longitude = formatter.format(longitude.toDouble())
                    */

                    /* Logs */
                    Log.d(LTAG, "Got longitude: $longitude")
                    Log.d(LTAG, "Got latitude: $latitude")
                    Log.d(LTAG, "Got accuracy: $accuracy")
                    Log.d(LTAG, "Got date: $locationDate")
                    Log.d(LTAG, "Got time: $locationTime")
                    Log.d(LTAG, "Got altitude: $altitude")

                    /* Update TextViews */
                    val strLatitude = "Latitude: $latitude"
                    latUpdateTextView.invalidate()
                    latUpdateTextView.setText(strLatitude)

                    val strLongitude = "Longitude: $longitude"
                    lngUpdateTextView.invalidate()
                    lngUpdateTextView.setText(strLongitude)

                    /* Update User with received device and location information */
                    user = user.copy(deviceID = deviceID, latitude = latitude.toDouble(),
                            longitude = longitude.toDouble(), horizontalAccuracy = accuracy.toDouble(),
                            date = locationDate, locationUpdateTime = locationTime, altitude = altitude.toDouble())

                    /* Update Firebase */
                    Log.d(LTAG, "Updating Firebase with Location Information")
                    firebaseConstructor.updateUserInfo(user)

                    /* Create JSON Object and update LTU database */
                    jsonMessage = Functions().createJSON(user)
                    Log.d(TAG, jsonMessage)
                    // Right now, the code updates the database everytime a new location value is received.
                    SendDeviceDetails().execute(url, jsonMessage)
                }

                "Acceleration" -> {
                    val acceleration = intent.getStringExtra("acceleration")
                    val accelerationUpdateTime = intent.getStringExtra("accelerationUpdateTime")
                    val strAcceleration = "Acceleration: $acceleration"

                    /* Logs */
                    //Log.d(RTAG, "Got acceleration: $acceleration")
                    //Log.d(RTAG, "Got acceleration update time: $accelerationUpdateTime")

                    /* Update the TextViews */
                    accUpdateTextView.invalidate()
                    accUpdateTextView.setText(strAcceleration)

                    /* Update User with received acceleration information */
                    user = user.copy(acceleration = acceleration.toDouble(), accelerationUpdateTime = accelerationUpdateTime)

                    /* Update User info in Firebase */
                    firebaseConstructor.updateUserInfo(user)

                    /* Create JSON Object and update LTU database */
                    jsonMessage = Functions().createJSON(user)
                    Log.d(TAG, jsonMessage)
                    //SendDeviceDetails().execute(url, jsonMessage)
                }

                "Pressure" -> {
                    /* This parameter is omitted for now.
                    * Uncomment its corresponding listener in 'SensorService' to access it.
                    */
                    val pressure = intent.getStringExtra("pressure")
                    var pressureUpdateTime = intent.getStringExtra("lightUpdateTime")

                    /* Update User with received pressure information */
                    user = user.copy(pressure = pressure.toDouble(), pressureUpdateTime = pressureUpdateTime)

                    /* Update User info in Firebase */
                    firebaseConstructor.updateUserInfo(user)
                }

                "Light" -> {
                    var lightLevel = intent.getStringExtra("lightLevel")
                    var lightUpdateTime = intent.getStringExtra("lightUpdateTime")

                    /* Update the TextViews */
                    val strLightLevel = "Ambient light (lx): $lightLevel"
                    lightUpdateTextView.invalidate()
                    lightUpdateTextView.setText(strLightLevel)


                    /* Update User with received light information */
                    user = user.copy(lightLevel = lightLevel.toInt(), lightUpdateTime = lightUpdateTime)

                    /* Update User info in Firebase */
                    firebaseConstructor.updateUserInfo(user)

                    /* Create JSON Object and update LTU database */
                    jsonMessage = Functions().createJSON(user)
                    Log.d(TAG, jsonMessage)
                    //SendDeviceDetails().execute(url, jsonMessage)
                }

                "Battery" -> {
                    // Check battery level
                    val batteryLevel = intent.getStringExtra("batteryLevel")
                    Log.d(BTAG, "BatteryLevel: " + batteryLevel)

                    // Check if battery is charging.
                    val isCharging = intent.getStringExtra("isCharging")
                    val usbCharge = intent.getStringExtra("usbCharge")
                    val acCharge = intent.getStringExtra("acCharge")
                    Log.d(BTAG, "Battery is charging: " + isCharging)
                    if (isCharging == "true") {
                        // If battery is charging. Which source?
                        Log.d(BTAG, "USB charge: " + usbCharge)
                        Log.d(BTAG, "AC charge: " + acCharge)
                    }
                    user = user.copy(batteryLevel = batteryLevel, isCharging = isCharging)

                    /* Update User info in Firebase */
                    firebaseConstructor.updateUserInfo(user)

                    /* Create JSON Object and update LTU database */
                    jsonMessage = Functions().createJSON(user)
                    Log.d(TAG, jsonMessage)
                    //SendDeviceDetails().execute(url, jsonMessage)
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

        /* Buttons */
        startButton = findViewById(R.id.button1)
        stopButton = findViewById(R.id.button2)

        startButton.setOnClickListener(this)
        stopButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button1 -> {
                startUpdates(v)
            }
            R.id.button2 -> {
                stopUpdates(v)
            }
        }
    }

    private fun initiateServices() {
        val deviceID = getDeviceID()
        Log.d(TAG, "Got ID: $deviceID")
        firebaseConstructor = FirebaseConstructor(deviceID)
        Log.d(TAG, "Created database instance with ID: $deviceID")

        /* TextView parameters */
        latUpdateTextView = findViewById(R.id.latTextView)
        lngUpdateTextView = findViewById(R.id.lngTextView)
        accUpdateTextView = findViewById(R.id.accTextView)
        lightUpdateTextView = findViewById(R.id.lightTextView)

        // Location Service
        locationServiceIntent = Intent(this, LocationUpdateService::class.java)
        locationServiceIntent!!.putExtra("deviceID", deviceID)

        // Sensor/Accelerometer Service
        sensorServiceIntent = Intent(this, SensorService::class.java)

        // Battery Service
        batteryServiceIntent = Intent(this, BatteryMonitoringService::class.java)

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
            startForegroundService(sensorServiceIntent)
            startForegroundService(batteryServiceIntent)
        } else {
            startService(locationServiceIntent)
            startService(sensorServiceIntent)
            startService(batteryServiceIntent)
        }

        // Local Broadcast manager
        val intentFilter = IntentFilter()
        intentFilter.addAction("Activity")
        intentFilter.addAction("Location")
        intentFilter.addAction("Acceleration")
        intentFilter.addAction("Pressure")
        intentFilter.addAction("Light")
        intentFilter.addAction("Battery")

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter)
    }

    private fun stopServices() {
        stopService(Intent(this, SensorService::class.java))
        stopService(Intent(this, LocationUpdateService::class.java))
        stopService(Intent(this, BatteryMonitoringService::class.java))
    }

    private fun startUpdates(v : View) {
        Toast.makeText(this, "Updating Database", Toast.LENGTH_LONG).show()
        if (isDevicePermissionGranted && isLocationPermissionGranted) {
            initiateServices()
        } else {
            // If Marshmallow+, ask for permission
            getLocationPermission()
            getDevicePermission()
        }
    }

    private fun stopUpdates(v : View) {
        Toast.makeText(this, "Stopping Updates", Toast.LENGTH_LONG).show()
        stopServices()
        SendDeviceDetails().cancel(true)
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

    // Get device ID, i.e. IMEI number
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

    // Display notifications for activity, e.g. "walking", "running", "in vehicle", etc.
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