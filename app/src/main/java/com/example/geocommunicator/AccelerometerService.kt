package com.example.geocommunicator

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.IBinder
import android.hardware.SensorManager
import android.content.Context.SENSOR_SERVICE
import androidx.core.content.ContextCompat.getSystemService




class AccelerometerService : Service(), SensorEventListener {

    // BAJS
    private val mSensorManager: SensorManager? = null
    var count = 0
    private val mSensorListener: ShakeEventListener? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //registering Sensor
        val sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI)

        //then you should return sticky
        return Service.START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}
