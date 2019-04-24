package com.example.geocommunicator

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.IBinder
import android.hardware.SensorManager
import android.content.Context.SENSOR_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.content.Context.SENSOR_SERVICE
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.text.SimpleDateFormat
import java.util.*

class SensorService : Service(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensorListener: SensorEventListener

    // Sensor parameters
    private var filteredPressure = 0.0f

    /* LP-Filter parameter.
    * Increasing the alpha value will make it more responsive to fluctuations,
    * lower alpha values will make it less noisy */
    private val alpha = 0.8

    // Control sampling information
    private val ACCELERATION_SAMPLING_RATE_MS = 500
    private val LIGHT_SAMPLING_RATE_MS = 500
    private val PRESSURE_SAMPLING_RATE_MS = 1000
    private val TEMPERATURE_SAMPLING_RATE_MS = 1000
    private var lastSaved = System.currentTimeMillis()
    private var lastEvent : Long = 0

    // Acceleration parameters
    private val gravity = DoubleArray(3)
    private val linear_acceleration = DoubleArray(3)

    // Velocity parameters
    private var vx = 0
    private var vy = 0
    private var vz = 0

    // Listen for shake events?
    //private lateinit var mSensorListener: ShakeEventListener

    private val TAG = "SensorService"
    var count = 0

    override fun onCreate() {
        Log.d(TAG, "Service started")
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //add this line only
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground()
        }

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Add new sensors here
        val accelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        /* Other sensors. These are omitted for now. */
        //val laccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        //val prSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        //val tempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        // Register sensors here
        //mSensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        /* Other sensors. These are omitted for now. */
        //mSensorManager.registerListener(this, laccSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, prSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)

        //then you should return sticky
        return Service.START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        /* Sensor types:
        * 1. Acceleration
        * 6. Pressure
        */
        val sensor = event.sensor

        /* Logs */
        //Log.d(TAG, "Sensor: " + sensor.toString())
        //Log.d(TAG, "Sensor Type: " + sensor.type.toString())
        //Log.d(TAG, "Sensor Pressure Type: " + Sensor.TYPE_PRESSURE.toString())

        /***
         * The Accelerometer returns raw accelerometer events, with minimal or no processing at all.
         * To determine acceleration in 2D, remove the gravity element from the sensor readings.
         */
        // Todo: Remove Gravity Element
        when (sensor.type) {

            Sensor.TYPE_ACCELEROMETER -> {
                if (System.currentTimeMillis() - lastSaved > ACCELERATION_SAMPLING_RATE_MS) {
                    // Get current time
                    lastSaved = System.currentTimeMillis()

                    // Determine the time difference between updates (deltaT)
                    val deltaT = lastSaved - lastEvent
                    lastEvent = lastSaved

                    // Get gravity samples
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                    // Get samples
                    val x = event.values[0].toDouble() - gravity[0]
                    val y = event.values[1].toDouble() - gravity[1]
                    val z = event.values[2].toDouble() - gravity[2]

                    // Calculate velocities in 3D
                    // Note: It's better to approximate the velocity based on GPS coordinates.
                    val vx = vx + deltaT * x
                    val vy = vy + deltaT * x
                    val vz = vz + deltaT * x

                    // Calculate acceleration and linear velocity
                    var acceleration = Math.sqrt(x * x + y * y + z * z)
                    var velocity = Math.sqrt(vx * vx + vy * vy + vz * vz)

                    // Set number of decimal points
                    acceleration = String.format(Locale.US, "%.3f", acceleration).toDouble()
                    velocity = String.format(Locale.US, "%.3f", velocity).toDouble()

                    // Extract Time information
                    val accEpochTime = (Date().getTime() - SystemClock.elapsedRealtime()) * 1000000 + event.timestamp
                    val accDateTime = Functions().epochToDate(accEpochTime / 1000000L)
                    val accelerationUpdateTime = accDateTime.second

                    // Debug
                    //Log.d(TAG, "acceleration: " + acceleration.toString())
                    //Log.d(TAG, "velocity: " + velocity.toString())

                    // Broadcast Intent
                    val intent = Intent("Acceleration")
                    intent.putExtra("acceleration", acceleration.toString())
                    intent.putExtra("accelerationUpdateTime", accelerationUpdateTime)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }

            Sensor.TYPE_PRESSURE -> {
                if (System.currentTimeMillis() - lastSaved > PRESSURE_SAMPLING_RATE_MS) {
                    val currentPressure = event.values[0].toDouble()
                    val pressure = (alpha * currentPressure) + (1 - alpha) * filteredPressure
                    //Log.d(TAG, "Pressure: " + pressure.toString())

                    // Extract Time information
                    val pressureEpochTime = (Date().getTime() - SystemClock.elapsedRealtime()) * 1000000 + event.timestamp
                    val pressureDateTime = Functions().epochToDate(pressureEpochTime / 1000000L)
                    val pressureUpdateTime = pressureDateTime.second

                    // Broadcast Intent
                    val intent = Intent("Pressure")
                    intent.putExtra("pressure", pressure.toString())
                    intent.putExtra("pressureUpdateTime", pressureUpdateTime)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
            Sensor.TYPE_LIGHT -> {
                //if (System.currentTimeMillis() - lastSaved > LIGHT_SAMPLING_RATE_MS) {
                    // Yields light level in lux (lx)
                    val lightLevel = event.values[0].toInt()
                    //Log.d(TAG, "Light Levels: " + lux)

                    // Extract Time information
                    val lightEpochTime = (Date().getTime() - SystemClock.elapsedRealtime()) * 1000000 + event.timestamp
                    val lightDateTime = Functions().epochToDate(lightEpochTime / 1000000L)
                    val lightUpdateTime = lightDateTime.second

                    // Broadcast Intent
                    val intent = Intent("Light")
                    intent.putExtra("lightLevel", lightLevel.toString())
                    intent.putExtra("lightUpdateTime", lightUpdateTime)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                //}
            }

            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                if (System.currentTimeMillis() - lastSaved > TEMPERATURE_SAMPLING_RATE_MS) {
                    // Not many phones have this. Useless?
                    Log.d(TAG, "Temperature!")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        // Will unregister all listeners
        mSensorManager.unregisterListener(this)
        super.onDestroy()
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
