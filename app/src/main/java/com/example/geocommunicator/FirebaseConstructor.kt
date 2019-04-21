package com.example.geocommunicator

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseConstructor constructor(userID: String) {

    private val onlineDriverDatabaseReference: DatabaseReference = FirebaseDatabase
        .getInstance()
        .reference
        .child(userID)
    init {
        onlineDriverDatabaseReference
            .onDisconnect()
            .removeValue()
    }
    fun updateUserInfo(user: User) {
        onlineDriverDatabaseReference
                .setValue(user)
        Log.e("UserInfo", " Updated")
    }
    fun updateValue(user: User, input : String) {
        onlineDriverDatabaseReference
                .setValue(user)
    }
    fun removeUser() {
        onlineDriverDatabaseReference
            .removeValue()
    }
}
