package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Schedules and dispatches customized local reminders:
 * 1. Online Rooms -> Reminds the user ONLY about their own remaining Para(s) or remaining Amal recitations.
 * 2. Local Monthly Qur'an Rooms -> Notifies the host when a new month is assigned so the new monthly
 *    Qur'an distribution is ready to read and share with everyone.
 */
object KhatmNotificationHelper {

    const val CHANNEL_ID_ONLINE_REMAINING = "khatm_online_remaining_channel"
    const val CHANNEL_ID_LOCAL_MONTHLY = "khatm_local_monthly_channel"

    private const val EXTRA_TITLE = "extra_khatm_title"
    private const val EXTRA_MESSAGE = "extra_khatm_message"
    private const val EXTRA_NOTIFICATION_ID = "extra_khatm_notification_id"
    private const val EXTRA_CHANNEL_ID = "extra_khatm_channel_id"
    private const val PREFS_NAME = "khatm_reminders_prefs"

    enum class ReminderPreset(val label: String, val subtitle: String, val daysFromNow: Int) {
        TOMORROW("Tomorrow (9:00 AM)", "Gentle daily check-in for your remaining portion", 1),
        THREE_DAYS("In 3 Days", "Mid-week reminder for your remaining assignment", 3),
        ONE_WEEK("In 7 Days", "Weekly check-in before the circle concludes", 7),
        END_OF_MONTH("End of Month (28th)", "Before the current monthly Khatm cycle ends", -1)
    }

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val onlineChannel = NotificationChannel(
                CHANNEL_ID_ONLINE_REMAINING,
                "My Remaining Para & Amal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personal reminders for your remaining assigned Paras and Amal recitations in Online Rooms"
            }

            val localMonthlyChannel = NotificationChannel(
                CHANNEL_ID_LOCAL_MONTHLY,
                "Monthly Qur'an Distribution Ready",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Monthly notifications when a new month's Qur'an rotation is assigned and ready to read and share"
            }

            manager.createNotificationChannel(onlineChannel)
            manager.createNotificationChannel(localMonthlyChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Schedules a customized reminder for an ONLINE room focusing strictly on the user's
     * own remaining uncompleted Para(s) or remaining Amal count.
     */
    fun scheduleOnlineRemainingReminder(
        context: Context,
        distributionId: Long,
        distributionName: String,
        memberName: String,
        remainingAssignmentSummary: String,
        preset: ReminderPreset
    ): String {
        ensureChannelCreated(context)

        val calendar = Calendar.getInstance()
        if (preset == ReminderPreset.END_OF_MONTH) {
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            if (currentDay >= 28) {
                calendar.add(Calendar.MONTH, 1)
            }
            calendar.set(Calendar.DAY_OF_MONTH, 28)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, preset.daysFromNow)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        }

        val triggerAtMillis = calendar.timeInMillis
        val notificationId = (distributionId.toInt() and 0x0FFFFFFF) + 4000

        val cleanMember = memberName.trim().ifBlank { "Reader" }
        val title = "$distributionName • Remaining Portion"
        val message = "Assalamu Alaikum $cleanMember, you still have $remainingAssignmentSummary remaining in \"$distributionName\". Tap to continue your reading."

        scheduleBroadcastAlarm(
            context = context,
            notificationId = notificationId,
            triggerAtMillis = triggerAtMillis,
            title = title,
            message = message,
            channelId = CHANNEL_ID_ONLINE_REMAINING
        )

        val dateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        val formattedTime = dateFormat.format(calendar.time)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("reminder_$distributionId", "$formattedTime ($remainingAssignmentSummary)")
            .apply()

        return formattedTime
    }

    /**
     * Schedules a customized monthly Qur'an rotation notification for a LOCAL room
     * whenever a new month is assigned (1st of the next month at 9:00 AM), telling the user
     * that the new monthly distribution is ready to read and share.
     */
    fun scheduleLocalMonthlyAssignmentReminder(
        context: Context,
        distributionId: Long,
        distributionName: String,
        nextMonthAssignmentPreview: String? = null
    ): String {
        ensureChannelCreated(context)

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val nextMonthLabel = monthNameFormat.format(calendar.time)

        val triggerAtMillis = calendar.timeInMillis
        val notificationId = (distributionId.toInt() and 0x0FFFFFFF) + 7000

        val title = "$distributionName • $nextMonthLabel Ready"
        val previewSuffix = if (!nextMonthAssignmentPreview.isNullOrBlank()) {
            " $nextMonthAssignmentPreview"
        } else {
            ""
        }
        val message = "New monthly Qur'an distribution for \"$distributionName\" ($nextMonthLabel) is now assigned and ready to read and share with your circle!$previewSuffix"

        scheduleBroadcastAlarm(
            context = context,
            notificationId = notificationId,
            triggerAtMillis = triggerAtMillis,
            title = title,
            message = message,
            channelId = CHANNEL_ID_LOCAL_MONTHLY
        )

        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val formattedTime = dateFormat.format(calendar.time)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("reminder_$distributionId", "Monthly Auto-Alert • $formattedTime")
            .apply()

        return formattedTime
    }

    private fun scheduleBroadcastAlarm(
        context: Context,
        notificationId: Int,
        triggerAtMillis: Long,
        title: String,
        message: String,
        channelId: String
    ) {
        val intent = Intent(context, KhatmDueReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_CHANNEL_ID, channelId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        try {
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, distributionId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        listOf(4000, 5000, 7000).forEach { offset ->
            val notificationId = (distributionId.toInt() and 0x0FFFFFFF) + offset
            val intent = Intent(context, KhatmDueReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("reminder_$distributionId")
            .apply()
    }

    fun getScheduledReminderLabel(context: Context, distributionId: Long): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("reminder_$distributionId", null)
    }

    /**
     * Dispatches an immediate notification when a new month is detected for a Local Monthly Qur'an distribution,
     * informing the user that the new month's distribution is assigned and ready to read and share.
     */
    fun notifyLocalNewMonthAssigned(
        context: Context,
        roomId: Long,
        roomName: String,
        monthLabel: String
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val notificationId = (roomId.toInt() and 0x0FFFFFFF) + 7500
        val title = "$roomName • $monthLabel Ready"
        val message = "New monthly Qur'an distribution for \"$roomName\" ($monthLabel) has been assigned and is ready to read and share!"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOCAL_MONTHLY)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Ignored if permission not granted
        }
    }

    fun handleScheduledAlarmBroadcast(context: Context, intent: Intent) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Qur'an Distribution Update"
        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: "Your assigned Qur'an reading is ready."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 5001)
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: CHANNEL_ID_ONLINE_REMAINING

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }
}
