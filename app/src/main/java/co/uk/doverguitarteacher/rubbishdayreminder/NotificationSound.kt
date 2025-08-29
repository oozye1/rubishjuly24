package co.uk.doverguitarteacher.rubbishdayreminder

/**
 * Defines all the available notification sounds for the app.
 * Each sound has a unique ID for storage, a user-friendly display name,
 * and a reference to its resource file in `res/raw`.
 */
enum class NotificationSound(val id: String, val displayName: String, val resourceId: Int?) {
    ALERT_HORN("alert_horn", "Horn", R.raw.alert_horn),
    ALERT_2("alert_2", "Alert 2", R.raw.alert2),
    ALERT_3("alert_3", "Alert 3", R.raw.alert3),
    ALERT_4("alert_4", "Alert 4", R.raw.alert4),
    ALERT_5("alert_5", "Alert 5", R.raw.alert5),
    ALERT_6("alert_6", "Alert 6", R.raw.alert6),
    ALERT_7("alert_7", "Alert 7", R.raw.alert7),
    SILENT("silent", "Silent", null); // The silent option has no resource

    companion object {
        /**
         * Finds a NotificationSound by its stored ID.
         * If the ID is not found (e.g., on first run), it defaults to ALERT_HORN.
         */
        fun fromId(id: String?): NotificationSound {
            return values().find { it.id == id } ?: ALERT_HORN
        }
    }
}
