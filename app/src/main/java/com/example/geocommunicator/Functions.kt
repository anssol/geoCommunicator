package com.example.geocommunicator

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
}
