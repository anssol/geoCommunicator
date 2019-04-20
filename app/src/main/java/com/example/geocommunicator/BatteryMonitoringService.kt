package com.example.geocommunicator

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.IBinder
import android.os.AsyncTask.execute
import android.os.BatteryManager
import android.text.format.DateUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class BatteryMonitoringService : Service() {

    private val TAG = "BatteryMonitoringService"
    private val BATTERY_UPDATE = "battery"

    val context = this@BatteryMonitoringService

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.hasExtra(BATTERY_UPDATE)) {
            BatteryCheckAsync().execute()
        }

        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private class BatteryCheckAsync: AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = BatteryMonitoringService().context.registerReceiver(null, ifilter)


            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            Log.d("BatteryInfo", "Battery is charging: $isCharging")

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            //Log.d("BatteryInfo", "Battery charge level: " + level / scale.toFloat())
            return null
        }

        fun onPostExecute() {
            BatteryMonitoringService().context.stopSelf()
        }
    }
}
