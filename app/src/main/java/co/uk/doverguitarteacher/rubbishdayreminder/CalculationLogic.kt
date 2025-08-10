package co.uk.doverguitarteacher.rubbishdayreminder

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

fun nextDateForBin(bin: BinType, settings: SettingsManager, today: LocalDate = LocalDate.now()): LocalDate {
    val dow: DayOfWeek = settings.getEffectiveCollectionDay(bin)
    var candidate = today.with(TemporalAdjusters.nextOrSame(dow))

    return when (settings.getBinFrequency(bin.id)) {
        Frequency.WEEKLY -> candidate
        Frequency.FORTNIGHTLY -> {
            val anchor = settings.getBinAnchorDate(bin.id)
            if (anchor == null) {
                candidate // missing anchor -> act weekly so user still gets reminders
            } else {
                val anchorAligned = if (anchor.dayOfWeek == dow) anchor
                else anchor.with(TemporalAdjusters.nextOrSame(dow))

                if (candidate.isBefore(anchorAligned)) {
                    anchorAligned
                } else {
                    val daysBetween = ChronoUnit.DAYS.between(anchorAligned, candidate)
                    val steps = (daysBetween / 14).toInt()
                    var next = anchorAligned.plusDays((steps * 14).toLong())
                    if (next.isBefore(candidate)) next = next.plusDays(14)
                    next
                }
            }
        }
    }
}

/** Earliest upcoming among ENABLED bins. Returns null if none enabled. */
fun nextUpcomingBinAndDate(settings: SettingsManager, today: LocalDate = LocalDate.now()): Pair<BinType, LocalDate>? {
    val enabled = BinTypes.ALL_BINS.filter { settings.isBinEnabled(it.id) }
    if (enabled.isEmpty()) return null
    val entries = enabled.map { bin -> bin to nextDateForBin(bin, settings, today) }
    return entries.minBy { it.second }
}
