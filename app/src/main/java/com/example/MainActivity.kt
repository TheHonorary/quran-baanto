package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.data.firebase.FirebaseCoreManager
import com.example.ui.MainScreen
import com.example.util.KhatmNotificationHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Ensure Firebase Auth, Firestore, and notification channels are initialized.
        // Notification runtime permission is NEVER requested on app launch — it is only
        // requested via a clean in-app opt-in dialog when creating/joining a distribution.
        KhatmNotificationHelper.ensureChannelCreated(applicationContext)
        FirebaseCoreManager.initialize(applicationContext)

        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}
