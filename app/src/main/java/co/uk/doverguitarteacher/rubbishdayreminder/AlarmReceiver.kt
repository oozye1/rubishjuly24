package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val binName = intent.getStringExtra("EXTRA_BIN_NAME") ?: "the bins"
        val timeOfDay = intent.getStringExtra("EXTRA_TIME_OF_DAY") ?: "soon"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(
            System.currentTimeMillis().toInt(),
            AlarmScheduler.buildAlarmNotification(context, binName, timeOfDay)
        )

        // Start the foreground service to play the alert multiple times
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("EXTRA_BIN_NAME", binName)
            putExtra("EXTRA_TIME_OF_DAY", timeOfDay)
            putExtra("EXTRA_REPEAT_COUNT", 5)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // (Optional) notify UI layer
        context.sendBroadcast(Intent("co.uk.doverguitarteacher.rubbishdayreminder.ALARM_UI"))

        // Re-arm next alarms
        val settingsManager = SettingsManager(context)
        AlarmScheduler.scheduleAlarms(context, settingsManager)
    }
}
