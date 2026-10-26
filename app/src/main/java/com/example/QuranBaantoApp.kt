package com.example

import android.app.Application
import com.example.data.firebase.FirebaseCoreManager
import com.example.util.KhatmNotificationHelper

class QuranBaantoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels and core Firebase (Auth + Firestore) on application startup
        KhatmNotificationHelper.ensureChannelCreated(this)
        FirebaseCoreManager.initialize(this)
    }
}
