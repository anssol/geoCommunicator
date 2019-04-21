package com.example.geocommunicator

data class User(var deviceID: String = "",
                val latitude: Double = 0.0,
                val longitude: Double = 0.0,
                val horizontalAccuracy: Double = 0.0,
                val speed: Float = 0F,
                val sampleDateTime: String = "",
                val altitude : Double = 0.0,
                val acceleration: Double = 0.0,
                val batteryLevel: String = "?",
                val isCharging: String = "?")
