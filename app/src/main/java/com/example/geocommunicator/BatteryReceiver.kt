package com.example.geocommunicator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class BatteryReceiver : BroadcastReceiver() {
    private val BTAG = "BatteryReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    Log.d(BTAG, "Battery has changed")
                    /*
                    val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL

                    val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                    val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                    Log.d(BTAG, "isCharging: " + isCharging.toString())
                    Log.d(BTAG, "chargePlug: " + chargePlug.toString())
                    Log.d(BTAG, "usbCharge" + usbCharge.toString())
                    Log.d(BTAG, "acCharge" + acCharge.toString())
                    */
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d(BTAG, "Power connected")
                    //User(batteryState = "charging")
                    //firebaseConstructor.updateUserInfo(User(batteryState = "charging"))
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(BTAG, "Power disconnected")
                    //User(batteryState = "disconnected")
                    //firebaseConstructor.updateUserInfo(User(batteryState = "disconnected"))
                }
                Intent.ACTION_BATTERY_LOW -> {
                    Log.d(BTAG, "Battery low")
                    //User(batteryState = "low")
                    //firebaseConstructor.updateUserInfo(User(batteryLevel = "low"))
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    Log.d(BTAG, "Battery Okay")
                    //User(batteryState = "ok")
                    //firebaseConstructor.updateUserInfo(User(batteryLevel = "ok"))
                }
            }
        }
    }
}