package com.example

import android.app.Application
import com.example.data.firebase.FirebaseCoreManager
import com.example.util.KhatmNotificationHelper
import com.example.util.ServerSupportAdManager
import com.google.android.gms.ads.MobileAds

class QuranBaantoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels and core Firebase (Auth + Firestore) on application startup
        KhatmNotificationHelper.ensureChannelCreated(this)
        FirebaseCoreManager.initialize(this)

        // Initialize Google Mobile Ads SDK on app launch and pre-load voluntary server support ad
        try {
            MobileAds.initialize(this) {
                ServerSupportAdManager.preloadAd(this)
            }
        } catch (_: Throwable) {
            // Graceful fallback if Google Play Services is not present on the device
        }
    }
}
