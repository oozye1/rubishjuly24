package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_final_v3", Context.MODE_PRIVATE)

    private val KEY_COLLECTION_DAY = "collection_day" // global fallback
    private val KEY_ANCHOR_BIN_ID = "anchor_bin_id"   // legacy, kept for compatibility

    // Global toggles
    private val KEY_EVENING_REMINDER_ENABLED = "evening_reminder_enabled"
    private val KEY_MORNING_REMINDER_ENABLED = "morning_reminder_enabled"

    // Times
    private val KEY_EVENING_REMINDER_HOUR = "evening_reminder_hour"
    private val KEY_EVENING_REMINDER_MINUTE = "evening_reminder_minute"
    private val KEY_MORNING_REMINDER_HOUR = "morning_reminder_hour"
    private val KEY_MORNING_REMINDER_MINUTE = "morning_reminder_minute"

    // Per-bin keys
    private fun keyBinDay(binId: String) = "collection_day_$binId"
    private fun keyBinFreq(binId: String) = "freq_$binId"                 // WEEKLY | FORTNIGHTLY
    private fun keyBinAnchorIso(binId: String) = "anchor_iso_$binId"      // yyyy-MM-dd

    // -------- Global fallback collection day --------
    fun saveCollectionDay(day: DayOfWeek) =
        prefs.edit().putString(KEY_COLLECTION_DAY, day.name).apply()

    fun getCollectionDay(): DayOfWeek =
        DayOfWeek.valueOf(prefs.getString(KEY_COLLECTION_DAY, DayOfWeek.TUESDAY.name)
            ?: DayOfWeek.TUESDAY.name)

    // -------- Legacy anchor bin rotation (kept, not used by new logic) --------
    fun saveCycleAnchor(anchorBin: BinType) =
        prefs.edit().putString(KEY_ANCHOR_BIN_ID, anchorBin.id).apply()

    fun getAnchorBin(): BinType =
        BinTypes.findById(prefs.getString(KEY_ANCHOR_BIN_ID, BinTypes.GENERAL.id))

    // -------- Global reminders + times --------
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

    fun isEveningReminderEnabled(): Boolean =
        prefs.getBoolean(KEY_EVENING_REMINDER_ENABLED, true)

    fun isMorningReminderEnabled(): Boolean =
        prefs.getBoolean(KEY_MORNING_REMINDER_ENABLED, false)

    fun getEveningReminderTime(): LocalTime =
        LocalTime.of(prefs.getInt(KEY_EVENING_REMINDER_HOUR, 19),
            prefs.getInt(KEY_EVENING_REMINDER_MINUTE, 0))

    fun getMorningReminderTime(): LocalTime =
        LocalTime.of(prefs.getInt(KEY_MORNING_REMINDER_HOUR, 7),
            prefs.getInt(KEY_MORNING_REMINDER_MINUTE, 0))

    // -------- Per-bin overrides --------
    fun getBinCollectionDay(binId: String): DayOfWeek? {
        val s = prefs.getString(keyBinDay(binId), null) ?: return null
        return runCatching { DayOfWeek.valueOf(s) }.getOrNull()
    }

    fun saveBinCollectionDay(binId: String, day: DayOfWeek?) {
        val e = prefs.edit()
        if (day == null) e.remove(keyBinDay(binId)) else e.putString(keyBinDay(binId), day.name)
        e.apply()
    }

    fun getEffectiveCollectionDay(bin: BinType): DayOfWeek {
        return getBinCollectionDay(bin.id) ?: getCollectionDay()
    }

    // -------- Per-bin frequency + anchor date --------
    fun getBinFrequency(binId: String): Frequency {
        val raw = prefs.getString(keyBinFreq(binId), null) ?: return Frequency.WEEKLY
        return runCatching { Frequency.valueOf(raw) }.getOrDefault(Frequency.WEEKLY)
    }

    fun saveBinFrequency(binId: String, freq: Frequency) {
        prefs.edit().putString(keyBinFreq(binId), freq.name).apply()
    }

    fun getBinAnchorDate(binId: String): LocalDate? {
        val iso = prefs.getString(keyBinAnchorIso(binId), null) ?: return null
        return runCatching { LocalDate.parse(iso) }.getOrNull()
    }

    fun saveBinAnchorDate(binId: String, date: LocalDate?) {
        val e = prefs.edit()
        if (date == null) e.remove(keyBinAnchorIso(binId)) else e.putString(keyBinAnchorIso(binId), date.toString())
        e.apply()
    }
}
