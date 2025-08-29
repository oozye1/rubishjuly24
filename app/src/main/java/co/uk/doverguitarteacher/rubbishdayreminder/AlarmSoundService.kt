package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class AlarmSoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var remainingPlays: Int = 0
    private var binName: String = "the bins"
    private var timeOfDay: String = "soon"
    // --- CHANGE 1: Add a variable to hold the sound resource ID ---
    private var soundResourceId: Int = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // --- CHANGE 2: Get the sound ID from the intent that started the service ---
        soundResourceId = intent?.getIntExtra("EXTRA_SOUND_RESOURCE_ID", -1) ?: -1

        // If the sound is "Silent" (ID is -1), do nothing and stop the service.
        if (soundResourceId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        binName = intent?.getStringExtra("EXTRA_BIN_NAME") ?: binName
        timeOfDay = intent?.getStringExtra("EXTRA_TIME_OF_DAY") ?: timeOfDay
        val total = intent?.getIntExtra("EXTRA_REPEAT_COUNT", 5) ?: 5
        remainingPlays = total

        startForeground(
            1,
            AlarmScheduler.buildPlaybackNotification(
                this, binName, timeOfDay,
                total - remainingPlays + 1, total
            )
        )

        playNext(total)
        return START_NOT_STICKY
    }

    private fun playNext(total: Int) {
        if (remainingPlays <= 0) {
            stopSelf(); return
        }
        mediaPlayer?.release()
        // --- CHANGE 3: Use the dynamic sound resource ID instead of the hardcoded one ---
        mediaPlayer = MediaPlayer.create(this, soundResourceId).apply {
            setOnCompletionListener {
                remainingPlays--
                if (remainingPlays > 0) {
                    val played = total - remainingPlays + 1
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(
                        1,
                        AlarmScheduler.buildPlaybackNotification(
                            this@AlarmSoundService, binName, timeOfDay, played, total
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
