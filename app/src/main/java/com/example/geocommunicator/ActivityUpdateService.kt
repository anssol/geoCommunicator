package com.example.geocommunicator

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityUpdateService : IntentService("ActivityUpdateService") {

    private val TAG = "ActivityRecognition"
    private var strConfidence = ""

    private fun setStrConfidence(strConfidence : String) {
        this.strConfidence = strConfidence
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "Handling Intent")
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result: ActivityRecognitionResult = ActivityRecognitionResult.extractResult(intent)
            handleDetectedActivities(result.probableActivities)
        }
    }

    // Confidence property indicates the likelihood that the user is performing the activity
    // represented in the result.
    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        for (activity in probableActivities) {
            setStrConfidence("activity.getConfidence() : " + activity.confidence)
            var strType = ""
            when (activity.type) {
                DetectedActivity.IN_VEHICLE -> {
                    Log.e(TAG, "In Vehicle: " + activity.confidence)
                    strType = "In Vehicle"
                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.e(TAG, "On Bicycle: " + activity.confidence)
                    strType = "On Bicycle"
                }
                DetectedActivity.ON_FOOT -> {
                    Log.e(TAG, "On Foot: " + activity.confidence)
                    strType = "On Foot"
                }
                DetectedActivity.RUNNING -> {
                    Log.e(TAG, "Running: " + activity.confidence)
                    strType = "Running"
                }
                DetectedActivity.STILL -> {
                    Log.e(TAG, "Still: " + activity.confidence)
                    strType = "Still"
                }
                DetectedActivity.TILTING -> {
                    Log.e(TAG, "Tilting: " + activity.confidence)
                    strType = "Tilting"
                }
                DetectedActivity.WALKING -> {
                    Log.e(TAG, "Walking: " + activity.confidence)
                    strType = "Walking"
                }
                DetectedActivity.UNKNOWN -> {
                    Log.e(TAG, "Unknown: " + activity.confidence)
                    strType = "Unknown"
                }
            }
            // Only send accurate activity notifications
            if (activity.confidence >= 75) {
                val intent = Intent("Activity")
                if (strType != "Still" && strType != "Unknown") {
                    intent.putExtra("Message", strType + " : " + activity.confidence)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
        }
    }
}