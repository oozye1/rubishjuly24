// Location: co/uk/doverguitarteacher/rubbishdayreminder/CalculationLogic.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun calculateBinForWeek(settingsManager: SettingsManager, collectionDay: DayOfWeek, weeksFromNow: Int): Pair<BinType, LocalDate> {
    val today = LocalDate.now()
    val lastWeeksBin = settingsManager.getAnchorBin()
    val cycle = BinTypes.ALL_BINS
    val anchorIndex = cycle.indexOf(lastWeeksBin)
    if (anchorIndex < 0) {
        val upcomingDate = today.with(TemporalAdjusters.nextOrSame(collectionDay)).plusWeeks(weeksFromNow.toLong())
        return Pair(BinTypes.GENERAL, upcomingDate)
    }
    val totalWeeksFromAnchor = 1 + weeksFromNow
    val upcomingBinIndex = (anchorIndex + totalWeeksFromAnchor) % cycle.size
    val upcomingBin = cycle[upcomingBinIndex]
    val upcomingDate = today.with(TemporalAdjusters.nextOrSame(collectionDay)).plusWeeks(weeksFromNow.toLong())
    return Pair(upcomingBin, upcomingDate)
}
