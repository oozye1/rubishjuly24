package co.uk.doverguitarteacher.rubbishdayreminder

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

// 1. Add the new "iconResId" property to the data class
data class BinType(
    val id: String,
    val displayName: String,
    val color: Color,
    @DrawableRes val iconResId: Int // <-- The new property
)

object BinTypes {
    // 2. Add the reference to the drawable for each bin
    val GENERAL = BinType("general", "Rubbish", Color(0xFF34495e), R.drawable.rubbish)
    val RECYCLING = BinType("recycling", "Recycling", Color(0xFF3498db), R.drawable.recycling)
    val GARDEN = BinType("garden", "Garden", Color(0xFF27ae60), R.drawable.garden)
    val FOOD = BinType("food", "Food", Color(0xFF8e44ad), R.drawable.food)

    val ALL_BINS = listOf(GENERAL, RECYCLING, GARDEN, FOOD)

    fun findById(id: String?): BinType {
        return ALL_BINS.find { it.id == id } ?: GENERAL
    }
}
