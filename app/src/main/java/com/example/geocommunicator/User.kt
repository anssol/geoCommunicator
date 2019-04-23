package com.example.geocommunicator

data class User(var deviceID: String = "",
                val latitude: Double = 0.0,
                val longitude: Double = 0.0,
                val horizontalAccuracy: Double = 0.0,
                val speed: Float = 0F,
                val altitude : Double = 0.0,
                val date: String = "",
                val locationUpdateTime: String = "",
                val acceleration: Double = 0.0,
                val accelerationUpdateTime: String = "",
                val pressure: Double = 0.0,
                val pressureUpdateTime: String = "",
                val lightLevel: Int = 0,
                val lightUpdateTime: String = "",
                val batteryLevel: String = "?",
                val isCharging: String = "?")
