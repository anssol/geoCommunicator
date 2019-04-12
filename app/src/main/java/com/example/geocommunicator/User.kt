package com.example.geocommunicator

// Todo: Make custom user ID
data class User(val latitude: String = "", val longitude: String = "", val horizontalAccuracy: String = "",
                val speed: String = "", val sampleDateTime: String = "", val altitude : String = "")

/*
data class User(var deviceID: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0, val horizontalAccuracy: Float = 0F,
                val speed: Float = 0F, val sampleDateTime: String = "", val altitude : Double = 0.0, val batteryState: String = "Missing",
                val batteryLevel: String = "ok")
*/