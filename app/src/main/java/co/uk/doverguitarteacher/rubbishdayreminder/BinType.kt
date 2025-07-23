// Location: co/uk/doverguitarteacher/rubbishdayreminder/BinType.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import androidx.compose.ui.graphics.Color

data class BinType(
    val id: String,
    val displayName: String,
    val color: Color,
    val iconResId: Int
)

object BinTypes {
    val RUBBISH = BinType(
        id = "RUBBISH",
        displayName = "Rubbish",
        color = Color(0xFF0000FF),
        iconResId = R.drawable.ic_trash_can
    )
    // alias for legacy code that still references GENERAL
    /** @deprecated Use RUBBISH instead */
    @Deprecated("Use RUBBISH", ReplaceWith("RUBBISH"))
    val GENERAL = RUBBISH

    val RECYCLING = BinType(
        id = "RECYCLING",
        displayName = "Recycling",
        color = Color(0xFF00FF00),
        iconResId = R.drawable.ic_recycle_b
    )
    val FOOD = BinType(
        id = "FOOD",
        displayName = "Food",
        color = Color(0xFFFF0000),
        iconResId = R.drawable.ic_food
    )
    val GARDEN = BinType(
        id = "GARDEN",
        displayName = "Garden",
        color = Color(0xFFA52A2A),
        iconResId = R.drawable.ic_garden
    )

    val ALL_BINS = listOf(
        RUBBISH,
        RECYCLING,
        FOOD,
        GARDEN
    )

    fun findById(id: String?): BinType =
        ALL_BINS.find { it.id == id } ?: RUBBISH
}
