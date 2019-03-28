package com.example.geocommunicator

// Todo: Make custom user ID
data class User(var deviceID: String, val latitude: Double, val longitude: Double, val horizontalAccuracy: Float,
                val speed: Float, val sampleDateTime: String, val altitude : Double)