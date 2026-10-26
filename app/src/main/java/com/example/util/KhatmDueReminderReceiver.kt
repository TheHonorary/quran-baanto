package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Internal BroadcastReceiver (android:exported="false") triggered by AlarmManager
 * when a scheduled Juz / Para, Amal, or Monthly Qur'an reminder fires.
 */
class KhatmDueReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        KhatmNotificationHelper.handleScheduledAlarmBroadcast(context, intent)
    }
}
