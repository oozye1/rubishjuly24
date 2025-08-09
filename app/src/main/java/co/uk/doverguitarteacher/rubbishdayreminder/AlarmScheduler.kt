package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlarmScheduler {
    const val EVENING_REQUEST_CODE = 101
    const val MORNING_REQUEST_CODE = 102

    private const val CHANNEL_ID = "bin_day_channel"
    private const val PLAYBACK_CHANNEL_ID = "bin_day_playback"

    fun scheduleAlarms(context: Context, settingsManager: SettingsManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelAlarm(context, EVENING_REQUEST_CODE)
        cancelAlarm(context, MORNING_REQUEST_CODE)

        // NEW LOGIC: get the earliest upcoming bin/date using per-bin Weekly/Fortnightly + Anchor
        val (bin, upcomingDate) = nextUpcomingBinAndDate(settingsManager)
        val binName = bin.displayName
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

        if (settingsManager.isEveningReminderEnabled()) {
            val t = settingsManager.getEveningReminderTime()
            val runAt = upcomingDate.minusDays(1).atTime(t)
            setExactAlarm(
                alarmManager,
                runAt,
                createPendingIntent(context, EVENING_REQUEST_CODE, binName, "at ${t.format(timeFormatter)}")
            )
        }

        if (settingsManager.isMorningReminderEnabled()) {
            val t = settingsManager.getMorningReminderTime()
            val runAt = upcomingDate.atTime(t)
            setExactAlarm(
                alarmManager,
                runAt,
                createPendingIntent(context, MORNING_REQUEST_CODE, binName, "at ${t.format(timeFormatter)}")
            )
        }
    }

    private fun setExactAlarm(
        alarmManager: AlarmManager,
        time: LocalDateTime,
        pendingIntent: PendingIntent
    ) {
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis > System.currentTimeMillis() && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                millis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createPendingIntent(context, requestCode, "", "")
        alarmManager.cancel(intent)
    }

    private fun createPendingIntent(
        context: Context,
        requestCode: Int,
        binName: String,
        timeOfDay: String
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_BIN_NAME", binName)
            putExtra("EXTRA_TIME_OF_DAY", timeOfDay)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri =
            Uri.parse("android.resource://${context.packageName}/${R.raw.alert_horn}")
        val soundChannel = NotificationChannel(
            CHANNEL_ID,
            "Bin Day Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for upcoming bin collections"
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            enableVibration(true)
        }

        val playbackChannel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            "Bin Day Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground service playing alarm sound repeats"
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(soundChannel)
        notificationManager.createNotificationChannel(playbackChannel)
    }

    internal fun buildAlarmNotification(
        context: Context,
        binName: String,
        timeOfDay: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trash_can)
            .setContentTitle("Bin Day Reminder!")
            .setContentText("Don't forget: It's $binName collection $timeOfDay.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    internal fun buildPlaybackNotification(
        context: Context,
        binName: String,
        timeOfDay: String,
        current: Int,
        total: Int
    ): Notification {
        return NotificationCompat.Builder(context, PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trash_can)
            .setContentTitle("Playing bin reminder ($current/$total)")
            .setContentText("It's $binName collection $timeOfDay.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
