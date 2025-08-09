package co.uk.doverguitarteacher.rubbishdayreminder

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Compute the next collection LocalDate for a given bin, based on:
 * - effective collection day (per-bin override or global)
 * - per-bin frequency (weekly/fortnightly)
 * - per-bin anchor date (required for fortnightly)
 *
 * If fortnightly and no anchor is set, we fall back to the next DOW (acts like weekly)
 * so the app still works; the UI will prompt to set an anchor.
 */
fun nextDateForBin(bin: BinType, settings: SettingsManager, today: LocalDate = LocalDate.now()): LocalDate {
    val dow: DayOfWeek = settings.getEffectiveCollectionDay(bin)
    var candidate = today.with(TemporalAdjusters.nextOrSame(dow))

    return when (settings.getBinFrequency(bin.id)) {
        Frequency.WEEKLY -> candidate
        Frequency.FORTNIGHTLY -> {
            val anchor = settings.getBinAnchorDate(bin.id)
            if (anchor == null) {
                // No anchor set yet — behave as weekly to avoid missed reminders
                candidate
            } else {
                // Align anchor to the selected DOW within its week (if user picked a wrong DOW)
                val anchorAligned = if (anchor.dayOfWeek == dow) anchor
                else anchor.with(TemporalAdjusters.nextOrSame(dow))

                if (candidate.isBefore(anchorAligned)) {
                    anchorAligned
                } else {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(anchorAligned, candidate)
                    val steps = (daysBetween / 14).toInt()
                    var next = anchorAligned.plusDays((steps * 14).toLong())
                    if (next.isBefore(candidate)) next = next.plusDays(14)
                    next
                }
            }
        }
    }
}

/** Return the earliest upcoming collection across all bins. */
fun nextUpcomingBinAndDate(settings: SettingsManager, today: LocalDate = LocalDate.now()): Pair<BinType, LocalDate> {
    val entries = BinTypes.ALL_BINS.map { bin -> bin to nextDateForBin(bin, settings, today) }
    return entries.minBy { it.second }
}
