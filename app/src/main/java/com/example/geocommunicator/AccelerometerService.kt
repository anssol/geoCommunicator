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
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.text.SimpleDateFormat
import java.util.*

class AccelerometerService : Service(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensorListener: SensorEventListener

    // Sensor parameters
    private var filteredPressure = 0.0f

    /* LP-Filter parameter.
    * Increasing the alpha value will make it more responsive to fluctuations,
    * lower alpha values will make it less noisy */
    private val alpha = 0.8

    // Control sampling information
    private val ACCELERATION_RATE_MS = 500
    private var lastSaved = System.currentTimeMillis()
    private var lastEvent : Long = 0

    // Velocity parameters
    private var vx = 0
    private var vy = 0
    private var vz = 0

    // Listen for shake events?
    //private lateinit var mSensorListener: ShakeEventListener

    private val TAG = "AccelerometerService"
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
        val accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        //val laccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val prSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        //val tempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        val lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Register sensors here
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, laccSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, prSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        //then you should return sticky
        return Service.START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        /* Sensor types:
        * 1. Acceleration
        * 6. Pressure
        */
        val sensor = event.sensor
        //Log.d(TAG, "Sensor: " + sensor.toString())
        //Log.d(TAG, "Sensor Type: " + sensor.type.toString())
        //Log.d(TAG, "Sensor Pressure Type: " + Sensor.TYPE_PRESSURE.toString())
        /***
         * The Accelerometer returns raw accelerometer events, with minimal or no processing at all.
         * To determine acceleration in 2D, remove the gravity element from the sensor readings.
         */
        when (sensor.type) {

            Sensor.TYPE_ACCELEROMETER -> {
                if (System.currentTimeMillis() - lastSaved > ACCELERATION_RATE_MS) {
                    // Get current time
                    lastSaved = System.currentTimeMillis()

                    // Determine the time difference between updates (deltaT)
                    val deltaT = lastSaved - lastEvent
                    lastEvent = lastSaved

                    // Get samples
                    val x = event.values[0].toDouble()
                    val y = event.values[1].toDouble()
                    val z = event.values[2].toDouble()

                    // Calculate velocities in 3D
                    // Note: It's better to approximate the velocity based on GPS coordinates.
                    val vx = vx + deltaT * x
                    val vy = vy + deltaT * x
                    val vz = vz + deltaT * x

                    // Calculate acceleration and linear velocity
                    var acceleration = Math.sqrt(x * x + y * y + z * z)
                    var velocity = Math.sqrt(vx * vx + vy * vy + vz * vz)

                    // Set number of decimal points
                    acceleration = String.format(Locale.US, "%.5f", acceleration).toDouble()
                    velocity = String.format(Locale.US, "%.5f", velocity).toDouble()

                    // Extract Time information
                    val accEpochTime = (Date().getTime() - SystemClock.elapsedRealtime()) * 1000000 + event.timestamp
                    val accDateTime = Functions().epochToDate(accEpochTime / 1000000L)
                    val accTime = accDateTime.second

                    // Debug
                    //Log.d(TAG, "acceleration: " + acceleration.toString())
                    //Log.d(TAG, "velocity: " + velocity.toString())

                    // Broadcast Intent
                    val intent = Intent("Acceleration")
                    intent.putExtra("acceleration", acceleration.toString())
                    intent.putExtra("accTime", accTime)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }

            Sensor.TYPE_PRESSURE -> {
                val currentPressure = event.values[0].toDouble()
                val pressure = (alpha * currentPressure) + (1 - alpha) * filteredPressure
                //Log.d(TAG, "Pressure: " + pressure.toString())
                // Broadcast Intent

                val intent = Intent("Pressure")
                intent.putExtra("pressure", pressure.toString())
                //intent.putExtra("accTime", pressureTime)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            Sensor.TYPE_LIGHT -> {
                // Yields light level in lux (lx)
                val lightLevel = event.values[0].toDouble()
                //Log.d(TAG, "Light Levels: " + lux)

                val intent = Intent("Light")
                intent.putExtra("lightLevel", lightLevel.toString())
                //intent.putExtra("accTime", pressureTime)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
        }

        /*
        if (sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Update acceleration at custom rate
            if (System.currentTimeMillis() - lastSaved > ACCELERATION_RATE_MS) {
                // Get current time
                lastSaved = System.currentTimeMillis()

                // Determine the time difference between updates (deltaT)
                val deltaT = lastSaved - lastEvent
                lastEvent = lastSaved

                // Get samples
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()

                // Calculate velocities in 3D
                // Note: It's better to approximate the velocity based on GPS coordinates.
                val vx = vx + deltaT * x
                val vy = vy + deltaT * x
                val vz = vz + deltaT * x

                // Calculate acceleration and linear velocity
                var acceleration = Math.sqrt(x * x + y * y + z * z)
                var velocity = Math.sqrt(vx * vx + vy * vy + vz * vz)

                // Set number of decimal points
                acceleration = String.format(Locale.US, "%.5f", acceleration).toDouble()
                velocity = String.format(Locale.US, "%.5f", velocity).toDouble()

                // Extract Time information
                val accEpochTime = (Date().getTime() - SystemClock.elapsedRealtime()) * 1000000 + event.timestamp
                val accDateTime = Functions().epochToDate(accEpochTime / 1000000L)
                val accTime = accDateTime.second

                // Debug
                //Log.d(TAG, "acceleration: " + acceleration.toString())
                //Log.d(TAG, "velocity: " + velocity.toString())

                // Broadcast Intent
                val intent = Intent("Acceleration")
                intent.putExtra("acceleration", acceleration.toString())
                intent.putExtra("accTime", accTime)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            else if (sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                Log.d(TAG, "Linear Acceleration")
            }

            else if (sensor.type == Sensor.TYPE_PRESSURE) {
                //Log.d(TAG, "Pressure")
                val currentPressure = event.values[0].toDouble()
                val pressure = (alpha * currentPressure) + (1 - alpha) * filteredPressure
                Log.d(TAG, "Pressure: " + pressure.toString())
            }

            else if (sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                Log.d(TAG, "Ambient Temperature")
                val ambientTemperature = event.values[0]
                //Log.d(TAG, "Temperature: " + ambientTemperature)
            }

        }
        */
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
