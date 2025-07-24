// Location: co.uk.doverguitarteacher.rubbishdayreminder/AlarmScheduler.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    const val EVENING_REQUEST_CODE = 101
    const val MORNING_REQUEST_CODE = 102

    private const val CHANNEL_ID = "bin_day_channel"            // With sound
    private const val PLAYBACK_CHANNEL_ID = "bin_day_playback"  // Silent foreground service channel

    fun scheduleAlarms(context: Context, settingsManager: SettingsManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelAlarm(context, EVENING_REQUEST_CODE)
        cancelAlarm(context, MORNING_REQUEST_CODE)

        val (upcomingBin, upcomingDate) =
            calculateBinForWeek(settingsManager, settingsManager.getCollectionDay(), 0)
        val binName = upcomingBin.displayName

        if (settingsManager.isEveningReminderEnabled()) {
            val eveningTime = upcomingDate.minusDays(1).atTime(19, 0)
            val eveningIntent =
                createPendingIntent(context, EVENING_REQUEST_CODE, binName, "This Evening")
            setExactAlarm(alarmManager, eveningTime, eveningIntent)
        }

        if (settingsManager.isMorningReminderEnabled()) {
            val morningTime = upcomingDate.atTime(7, 0)
            val morningIntent =
                createPendingIntent(context, MORNING_REQUEST_CODE, binName, "This Morning")
            setExactAlarm(alarmManager, morningTime, morningIntent)
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

    /**
     * Creates both the sound channel (for the alarm notification) and a silent channel
     * for the foreground service that plays the sound multiple times.
     */
    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel with custom sound (fires once when notification posted)
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

        // Silent channel for foreground service playback (no extra sound)
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

/**
 * BroadcastReceiver triggered by AlarmManager.
 * Posts the initial notification (which plays alert_horn once via channel)
 * then starts a foreground service to play the sound 5 total times.
 * Also broadcasts an intent so the visible UI can show confetti.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val binName = intent.getStringExtra("EXTRA_BIN_NAME") ?: "the bins"
        val timeOfDay = intent.getStringExtra("EXTRA_TIME_OF_DAY") ?: "soon"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Post initial notification (channel tone plays once here)
        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            AlarmScheduler.buildAlarmNotification(context, binName, timeOfDay)
        )

        // Start service to play sound 5 times
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("EXTRA_BIN_NAME", binName)
            putExtra("EXTRA_TIME_OF_DAY", timeOfDay)
            putExtra("EXTRA_REPEAT_COUNT", 5) // total plays
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Notify UI (if open) to trigger confetti
        context.sendBroadcast(
            Intent("co.uk.doverguitarteacher.rubbishdayreminder.ALARM_UI")
        )

        // Re-schedule next alarms
        val settingsManager = SettingsManager(context)
        AlarmScheduler.scheduleAlarms(context, settingsManager)
    }
}

/**
 * Foreground service that plays the alert MP3 repeatedly (N times).
 * We manually loop so we can stop automatically after the desired count.
 */
class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var remainingPlays: Int = 0
    private var binName: String = "the bins"
    private var timeOfDay: String = "soon"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        binName = intent?.getStringExtra("EXTRA_BIN_NAME") ?: binName
        timeOfDay = intent?.getStringExtra("EXTRA_TIME_OF_DAY") ?: timeOfDay
        val total = intent?.getIntExtra("EXTRA_REPEAT_COUNT", 5) ?: 5
        remainingPlays = total

        // Start foreground immediately
        startForeground(
            1,
            AlarmScheduler.buildPlaybackNotification(
                this,
                binName,
                timeOfDay,
                total - remainingPlays + 1,
                total
            )
        )

        playNext(total)
        return START_NOT_STICKY
    }

    private fun playNext(total: Int) {
        if (remainingPlays <= 0) {
            stopSelf()
            return
        }

        // Release previous instance if any
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_horn).apply {
            setOnCompletionListener {
                remainingPlays--
                if (remainingPlays > 0) {
                    // Update notification progress
                    val played = total - remainingPlays + 1
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(
                        1,
                        AlarmScheduler.buildPlaybackNotification(
                            this@AlarmSoundService,
                            binName,
                            timeOfDay,
                            played,
                            total
                        )
                    )
                    playNext(total)
                } else {
                    stopSelf()
                }
            }
            start()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
