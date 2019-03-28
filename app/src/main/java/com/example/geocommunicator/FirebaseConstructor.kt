package com.example.geocommunicator

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseConstructor constructor() {

    private val onlineDriverDatabaseReference: DatabaseReference = FirebaseDatabase
        .getInstance()
        .reference
        .child("online_users")
    //    .child(userId)
    //    .child("UserActivity")
    init {
        onlineDriverDatabaseReference
            .onDisconnect()
            .removeValue()
    }
    fun updateUserInfo(user: User) {
        onlineDriverDatabaseReference
            .setValue(user)
        Log.e("User Info", " Updated")
    }
    fun removeUser() {
        onlineDriverDatabaseReference
            .removeValue()
    }
}
