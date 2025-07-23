package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.Context
import java.time.DayOfWeek

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_final_v2", Context.MODE_PRIVATE)

    private val KEY_COLLECTION_DAY = "collection_day"
    private val KEY_ANCHOR_BIN_ID = "anchor_bin_id" // This key is no longer used but can remain
    private val KEY_EVENING_REMINDER_ENABLED = "evening_reminder_enabled"
    private val KEY_MORNING_REMINDER_ENABLED = "morning_reminder_enabled"

    fun saveCollectionDay(day: DayOfWeek) =
        prefs.edit().putString(KEY_COLLECTION_DAY, day.name).apply()

    // This function is no longer called from the UI but can be kept if needed elsewhere
    fun saveCycleAnchor(anchorBin: BinType) =
        prefs.edit().putString(KEY_ANCHOR_BIN_ID, anchorBin.id).apply()

    fun saveReminderSettings(evening: Boolean, morning: Boolean) {
        prefs.edit()
            .putBoolean(KEY_EVENING_REMINDER_ENABLED, evening)
            .putBoolean(KEY_MORNING_REMINDER_ENABLED, morning)
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

    /** Retrieve a custom collection day for a specific bin type, or null if none. */
    fun getBinCollectionDay(binId: String): DayOfWeek? {
        val key = "collection_day_$binId"
        val stored = prefs.getString(key, null) ?: return null
        return try {
            DayOfWeek.valueOf(stored)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * **[CORRECTED]**
     * Save (or clear if null) a custom collection day override for a bin.
     */
    fun saveBinCollectionDay(binId: String, day: DayOfWeek?) {
        val key = "collection_day_$binId"
        val editor = prefs.edit() // Get the editor first
        if (day == null) {
            editor.remove(key)
        } else {
            editor.putString(key, day.name)
        }
        editor.apply() // Commit the changes with a single apply() call
    }

    /**
     * **[NEW HELPER FUNCTION]**
     * Gets the effective collection day for a bin.
     * It returns the per-bin override if one exists, otherwise it returns the global collection day.
     */
    fun getEffectiveCollectionDay(bin: BinType): DayOfWeek {
        val overrideDay = getBinCollectionDay(bin.id)
        // If an override day was found, return it. Otherwise, return the global default.
        return overrideDay ?: getCollectionDay()
    }
}
