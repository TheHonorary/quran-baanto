package com.example

import android.app.Application
import com.example.data.firebase.FirebaseCoreManager
import com.example.util.KhatmNotificationHelper
import com.example.util.ServerSupportAdManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuranBaantoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels and core Firebase (Auth + Firestore) on application startup
        KhatmNotificationHelper.ensureChannelCreated(this)
        FirebaseCoreManager.initialize(this)

        // Initialize Google Mobile Ads SDK asynchronously on background thread to prevent blocking main thread
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@QuranBaantoApp) {
                    ServerSupportAdManager.preloadAd(this@QuranBaantoApp)
                }
            } catch (_: Throwable) {
                // Graceful fallback if Google Play Services is not present on the device
            }
        }
    }
}
