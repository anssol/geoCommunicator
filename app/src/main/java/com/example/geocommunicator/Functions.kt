package com.example.geocommunicator

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

class Functions {
/*
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

    fun getDevicePermission(activity: Activity) : Boolean {
        val devicePermission = android.Manifest.permission.READ_PHONE_STATE
        val permissionCode = Constants.DEVICE_PERMISSION_REQUEST_CODE
        return ContextCompat.checkSelfPermission(activity, devicePermission) == PackageManager.PERMISSION_GRANTED
    }


    fun getDevicePermission_old(activity: Activity) {
        val devicePermission = android.Manifest.permission.READ_PHONE_STATE
        val permissionCode = Constants.DEVICE_PERMISSION_REQUEST_CODE
        if (ContextCompat.checkSelfPermission(activity, devicePermission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Gets whether you should show UI with rationale for requesting a permission.
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, devicePermission)) {

            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(activity, arrayOf(devicePermission), permissionCode)
            }
        } else {
            return
        }
    }
    */
}
