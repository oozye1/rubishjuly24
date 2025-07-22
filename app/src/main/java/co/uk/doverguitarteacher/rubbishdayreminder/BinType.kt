// Location: co/uk/doverguitarteacher/rubbishdayreminder/BinType.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import androidx.compose.ui.graphics.Color

data class BinType(val id: String, val displayName: String, val color: Color, val iconResId: Int)
object BinTypes {
    val GENERAL = BinType(id = "GENERAL", displayName = "General Waste", color = Color(0xFF34495e), iconResId = R.drawable.ic_trash_can)
    val RECYCLING_FOOD = BinType(id = "RECYCLING_FOOD", displayName = "Recycling & Food", color = Color(0xFF16a085), iconResId = R.drawable.ic_recycle_b)
    val ALL_BINS = listOf(GENERAL, RECYCLING_FOOD)
    fun findById(id: String?): BinType = ALL_BINS.find { it.id == id } ?: GENERAL
}
