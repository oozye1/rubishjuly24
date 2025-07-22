// Location: co/uk/doverguitarteacher/rubbishdayreminder/AlarmScheduler.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    const val EVENING_REQUEST_CODE = 101; const val MORNING_REQUEST_CODE = 102; private const val CHANNEL_ID = "bin_day_channel"
    fun scheduleAlarms(context: Context, settingsManager: SettingsManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAlarm(context, EVENING_REQUEST_CODE); cancelAlarm(context, MORNING_REQUEST_CODE)
        val (upcomingBin, upcomingDate) = calculateBinForWeek(settingsManager, settingsManager.getCollectionDay(), 0)
        val binName = upcomingBin.displayName
        if (settingsManager.isEveningReminderEnabled()) {
            val eveningTime = upcomingDate.minusDays(1).atTime(19, 0)
            val eveningIntent = createPendingIntent(context, EVENING_REQUEST_CODE, binName, "This Evening")
            setExactAlarm(alarmManager, eveningTime, eveningIntent)
        }
        if (settingsManager.isMorningReminderEnabled()) {
            val morningTime = upcomingDate.atTime(7, 0)
            val morningIntent = createPendingIntent(context, MORNING_REQUEST_CODE, binName, "This Morning")
            setExactAlarm(alarmManager, morningTime, morningIntent)
        }
    }
    private fun setExactAlarm(alarmManager: AlarmManager, time: LocalDateTime, pendingIntent: PendingIntent) {
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis > System.currentTimeMillis() && alarmManager.canScheduleExactAlarms()) { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent) }
    }
    private fun cancelAlarm(context: Context, requestCode: Int) { val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val intent = createPendingIntent(context, requestCode, "", ""); alarmManager.cancel(intent) }
    private fun createPendingIntent(context: Context, requestCode: Int, binName: String, timeOfDay: String): PendingIntent { val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("EXTRA_BIN_NAME", binName); putExtra("EXTRA_TIME_OF_DAY", timeOfDay) }; return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
    fun createNotificationChannel(context: Context) {
        val name = "Bin Day Reminders"; val descriptionText = "Notifications for upcoming bin collections"; val importance = NotificationManager.IMPORTANCE_HIGH; val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.alert_horn}"); val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText; setSound(soundUri, AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ALARM).build()) }; val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; notificationManager.createNotificationChannel(channel)
    }
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val binName = intent.getStringExtra("EXTRA_BIN_NAME") ?: "the bins"; val timeOfDay = intent.getStringExtra("EXTRA_TIME_OF_DAY") ?: "soon"; val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "bin_day_channel").setSmallIcon(R.drawable.ic_trash_can).setContentTitle("Bin Day Reminder!").setContentText("Don't forget: It's $binName collection $timeOfDay.").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification); val settingsManager = SettingsManager(context); AlarmScheduler.scheduleAlarms(context, settingsManager)
    }
}
