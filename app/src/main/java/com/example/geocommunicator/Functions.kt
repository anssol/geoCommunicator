package com.example.geocommunicator

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Functions {
    /**
     * Defines the set of global functions used in this project.
     */

    fun epochToDate(timestamp : Long): Pair<String, String> {
        val date: Date = java.util.Date(timestamp)
        val sdf: SimpleDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd")
        val stf: SimpleDateFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS")
        val formattedDate = sdf.format(date)
        val formattedTime = stf.format(date)
        return Pair(formattedDate, formattedTime)
    }

    fun createJSON(user : User) : String {

        val item = JSONObject()
        item.put("type", "Gateway")
        item.put("isPattern", "false")
        item.put("id", "UppsalaUni_01")

        // Create attributes
        val imei = JSONObject()
        imei.put("name", "imei")
        imei.put("type", "String")
        imei.put("value", user.deviceID)

        val jLocationDate = JSONObject()
        jLocationDate.put("name", "date")
        jLocationDate.put("type", "String")
        jLocationDate.put("value", user.date)

        val jLatitude = JSONObject()
        jLatitude.put("name", "latitude")
        jLatitude.put("type", "Double")
        jLatitude.put("value", user.latitude)

        val jLongitude = JSONObject()
        jLongitude.put("name", "longitude")
        jLongitude.put("type", "Double")
        jLongitude.put("value", user.longitude)

        val jAltitude = JSONObject()
        jAltitude.put("name", "altitude")
        jAltitude.put("type", "Double")
        jAltitude.put("value", user.altitude)

        val jHorizontalAccuracy = JSONObject()
        jHorizontalAccuracy.put("name", "horizontalAccuracy")
        jHorizontalAccuracy.put("type", "Double")
        jHorizontalAccuracy.put("value", user.horizontalAccuracy)

        val jLocationTime = JSONObject()
        jLocationTime.put("name", "locationUpdateTime")
        jLocationTime.put("type", "String")
        jLocationTime.put("value", user.locationUpdateTime)

        val jAcceleration = JSONObject()
        jAcceleration.put("name", "acceleration")
        jAcceleration.put("type", "Double")
        jAcceleration.put("value", user.acceleration)

        val jAccelerationTime = JSONObject()
        jAccelerationTime.put("name", "accelerationUpdateTime")
        jAccelerationTime.put("type", "String")
        jAccelerationTime.put("value", user.accelerationUpdateTime)

        val jLightLevel = JSONObject()
        jLightLevel.put("name", "lightLevel")
        jLightLevel.put("type", "Int")
        jLightLevel.put("value", user.lightLevel)

        val jLightTime = JSONObject()
        jLightTime.put("name", "lightUpdateTime")
        jLightTime.put("type", "String")
        jLightTime.put("value", user.lightUpdateTime)

        val jBatteryLevel = JSONObject()
        jBatteryLevel.put("name", "batteryLevel")
        jBatteryLevel.put("type", "String")
        jBatteryLevel.put("value", user.batteryLevel)

        val jBatteryChargeStatus = JSONObject()
        jBatteryChargeStatus.put("name", "isCharging")
        jBatteryChargeStatus.put("type", "String")
        jBatteryChargeStatus.put("value", user.isCharging)

        val attributes = JSONArray()
        attributes.put(imei)
        attributes.put(jLocationDate)
        attributes.put(jLatitude)
        attributes.put(jLongitude)
        attributes.put(jLocationTime)
        attributes.put(jAltitude)
        attributes.put(jHorizontalAccuracy)
        attributes.put(jLocationTime)
        attributes.put(jAcceleration)
        attributes.put(jAccelerationTime)
        attributes.put(jLightLevel)
        attributes.put(jLightTime)
        attributes.put(jBatteryLevel)
        attributes.put(jBatteryChargeStatus)

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
}
