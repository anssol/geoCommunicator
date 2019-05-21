package com.example.geocommunicator

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.Handler
import androidx.core.app.NotificationCompat
import android.content.Context
import android.graphics.Color
import androidx.annotation.RequiresApi

class BatteryMonitoringService : Service() {

    private val TAG = "BatteryService"
    private lateinit var handler: Handler
    private lateinit var functions: Functions

    // Check battery status every X milliseconds
    private val CHECK_BATTERY_INTERVAL : Long = 5000

    override fun onCreate() {
        handler = Handler()
        handler.postDelayed(checkBatteryStatusRunnable, CHECK_BATTERY_INTERVAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            //functions.startForeground(this)
            startForeground()
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkBatteryStatusRunnable)
    }

    private val checkBatteryStatusRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Checking battery status")
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                this@BatteryMonitoringService.registerReceiver(null, ifilter)
            }

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // How are we charging?
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            var batteryLevel = 0
            if (level != null && scale != null) {
                batteryLevel = ((level / scale.toDouble()) * 100).toInt()
            }
            //Log.d(TAG, "BatteryLevel: " + batteryLevel.toString())

            // schedule next battery check
            handler.postDelayed(this, CHECK_BATTERY_INTERVAL)
            //Log.e(TAG, level.toString() + "mm cached")

            // Broadcast Intent
            val intent = Intent("Battery")
            intent.putExtra("isCharging", isCharging.toString())
            intent.putExtra("usbCharge", usbCharge.toString())
            intent.putExtra("acCharge", acCharge.toString())
            intent.putExtra("batteryLevel", batteryLevel.toString())
            LocalBroadcastManager.getInstance(this@BatteryMonitoringService).sendBroadcast(intent)
        }
    }

    private fun startForeground() {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("my_service", "My Background Service")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        startForeground(1, NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_icon_background)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running in background")
                .setContentIntent(pendingIntent)
                .build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}
