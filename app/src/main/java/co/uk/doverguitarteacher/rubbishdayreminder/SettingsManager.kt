package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_final_v2", Context.MODE_PRIVATE)

    private val KEY_COLLECTION_DAY = "collection_day"
    private val KEY_ANCHOR_BIN_ID = "anchor_bin_id"
    private val KEY_EVENING_REMINDER_ENABLED = "evening_reminder_enabled"
    private val KEY_MORNING_REMINDER_ENABLED = "morning_reminder_enabled"

    // Time storage keys
    private val KEY_EVENING_REMINDER_HOUR = "evening_reminder_hour"
    private val KEY_EVENING_REMINDER_MINUTE = "evening_reminder_minute"
    private val KEY_MORNING_REMINDER_HOUR = "morning_reminder_hour"
    private val KEY_MORNING_REMINDER_MINUTE = "morning_reminder_minute"

    fun saveCollectionDay(day: DayOfWeek) =
        prefs.edit().putString(KEY_COLLECTION_DAY, day.name).apply()

    fun saveCycleAnchor(anchorBin: BinType) =
        prefs.edit().putString(KEY_ANCHOR_BIN_ID, anchorBin.id).apply()

    /**
     * Persist booleans AND chosen times.
     */
    fun saveReminderSettings(
        eveningEnabled: Boolean,
        morningEnabled: Boolean,
        eveningTime: LocalTime,
        morningTime: LocalTime
    ) {
        prefs.edit()
            .putBoolean(KEY_EVENING_REMINDER_ENABLED, eveningEnabled)
            .putBoolean(KEY_MORNING_REMINDER_ENABLED, morningEnabled)
            .putInt(KEY_EVENING_REMINDER_HOUR, eveningTime.hour)
            .putInt(KEY_EVENING_REMINDER_MINUTE, eveningTime.minute)
            .putInt(KEY_MORNING_REMINDER_HOUR, morningTime.hour)
            .putInt(KEY_MORNING_REMINDER_MINUTE, morningTime.minute)
            .apply()
    }

    fun getCollectionDay(): DayOfWeek =
        DayOfWeek.valueOf(
            prefs.getString(KEY_COLLECTION_DAY, DayOfWeek.TUESDAY.name)
                ?: DayOfWeek.TUESDAY.name
        )

    fun getAnchorBin(): BinType =
        BinTypes.findById(
            prefs.getString(KEY_ANCHOR_BIN_ID, BinTypes.GENERAL.id)
        )

    fun isEveningReminderEnabled(): Boolean =
        prefs.getBoolean(KEY_EVENING_REMINDER_ENABLED, true)

    fun isMorningReminderEnabled(): Boolean =
        prefs.getBoolean(KEY_MORNING_REMINDER_ENABLED, false)

    // Retrieve persisted times (defaults: evening 19:00, morning 07:00)
    fun getEveningReminderTime(): LocalTime =
        LocalTime.of(
            prefs.getInt(KEY_EVENING_REMINDER_HOUR, 19),
            prefs.getInt(KEY_EVENING_REMINDER_MINUTE, 0)
        )

    fun getMorningReminderTime(): LocalTime =
        LocalTime.of(
            prefs.getInt(KEY_MORNING_REMINDER_HOUR, 7),
            prefs.getInt(KEY_MORNING_REMINDER_MINUTE, 0)
        )

    /** Retrieve a custom collection day for a specific bin type, or null if none. */
    fun getBinCollectionDay(binId: String): DayOfWeek? {
        val key = "collection_day_$binId"
        val stored = prefs.getString(key, null) ?: return null
        return try {
            DayOfWeek.valueOf(stored)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun saveBinCollectionDay(binId: String, day: DayOfWeek?) {
        val key = "collection_day_$binId"
        val editor = prefs.edit()
        if (day == null) {
            editor.remove(key)
        } else {
            editor.putString(key, day.name)
        }
        editor.apply()
    }

    fun getEffectiveCollectionDay(bin: BinType): DayOfWeek {
        val overrideDay = getBinCollectionDay(bin.id)
        return overrideDay ?: getCollectionDay()
    }
}
