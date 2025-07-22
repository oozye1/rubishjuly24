// Location: co/uk/doverguitarteacher/rubbishdayreminder/AppSettings.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.Context
import java.time.DayOfWeek

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_v_final_correct", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_COLLECTION_DAY = "collection_day"
        const val KEY_ANCHOR_BIN_ID = "anchor_bin_id"
        const val KEY_EVENING_REMINDER_ENABLED = "evening_reminder_enabled"
        const val KEY_MORNING_REMINDER_ENABLED = "morning_reminder_enabled"
    }

    fun saveCollectionDay(day: DayOfWeek) = prefs.edit().putString(KEY_COLLECTION_DAY, day.name).apply()
    fun saveCycleAnchor(anchorBin: BinType) = prefs.edit().putString(KEY_ANCHOR_BIN_ID, anchorBin.id).apply()
    fun saveReminderSettings(evening: Boolean, morning: Boolean) {
        prefs.edit()
            .putBoolean(KEY_EVENING_REMINDER_ENABLED, evening)
            .putBoolean(KEY_MORNING_REMINDER_ENABLED, morning)
            .apply()
    }

    fun getCollectionDay(): DayOfWeek = DayOfWeek.valueOf(prefs.getString(KEY_COLLECTION_DAY, DayOfWeek.TUESDAY.name) ?: DayOfWeek.TUESDAY.name)
    fun getAnchorBin(): BinType = BinTypes.findById(prefs.getString(KEY_ANCHOR_BIN_ID, BinTypes.GENERAL.id))
    fun isEveningReminderEnabled(): Boolean = prefs.getBoolean(KEY_EVENING_REMINDER_ENABLED, true)
    fun isMorningReminderEnabled(): Boolean = prefs.getBoolean(KEY_MORNING_REMINDER_ENABLED, false)
}
