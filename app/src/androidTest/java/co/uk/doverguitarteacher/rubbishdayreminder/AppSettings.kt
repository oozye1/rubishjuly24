// Location: co/uk/doverguitarteacher/rubbishdayreminder/AppSettings.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// --- Part 1: The Settings Logic Class ---
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_final", Context.MODE_PRIVATE)

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

// --- Part 2: The Settings UI Composable ---
@Composable
fun AppSettingsScreen(
    appSettings: AppSettings,
    onNavigateBack: () -> Unit
) {
    var selectedDay by remember { mutableStateOf(appSettings.getCollectionDay()) }
    var anchorBin by remember { mutableStateOf(appSettings.getAnchorBin()) }
    var eveningReminder by remember { mutableStateOf(appSettings.isEveningReminderEnabled()) }
    var morningReminder by remember { mutableStateOf(appSettings.isMorningReminderEnabled()) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2c3e50)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))
        SettingRow(label = "My Collection Day Is...") { DayOfWeekSelector(selectedDay) { selectedDay = it } }
        Spacer(Modifier.height(16.dp))
        SettingRow(label = "Last Week's Collection Was...") { BinTypeSelector(anchorBin) { anchorBin = it } }
        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray)
        Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))
        ReminderRow("Evening Reminder (7 PM)", eveningReminder) { eveningReminder = it }
        ReminderRow("Morning Reminder (7 AM)", morningReminder) { morningReminder = it }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onNavigateBack, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe74c3c))) { Text("Cancel") }
            Button(
                onClick = {
                    appSettings.saveCollectionDay(selectedDay)
                    appSettings.saveCycleAnchor(anchorBin)
                    appSettings.saveReminderSettings(eveningReminder, morningReminder)
                    onNavigateBack()
                },
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ecc71))) { Text("Save") }
        }
    }
}

// --- Part 3: Reusable UI components for the settings screen ---
@Composable fun ReminderRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(text = label, color = Color.White, fontSize = 16.sp); Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2ecc71), uncheckedThumbColor = Color.LightGray, uncheckedTrackColor = Color.Gray)) } }
@Composable fun SettingRow(label: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth()) { Text(label, color = Color(0xFFbdc3c7), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)); content() } }
@Composable fun DayOfWeekSelector(selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.Gray, RoundedCornerShape(12.dp)).clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Color.White, fontSize = 16.sp); Icon(Icons.Default.ArrowDropDown, "Open day selector", tint = Color.White) }; DropdownMenu(expanded, { expanded = false }, Modifier.background(Color(0xFF34495e))) { DayOfWeek.values().forEach { day -> DropdownMenuItem(text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Color.White) }, onClick = { onDaySelected(day); expanded = false }) } } } }
@Composable fun BinTypeSelector(selectedBin: BinType, onBinSelected: (BinType) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.Gray, RoundedCornerShape(12.dp)).clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(selectedBin.displayName, color = Color.White, fontSize = 16.sp); Icon(Icons.Default.ArrowDropDown, "Open bin selector", tint = Color.White) }; DropdownMenu(expanded, { expanded = false }, Modifier.background(Color(0xFF34495e))) { BinTypes.ALL_BINS.forEach { bin -> DropdownMenuItem(text = { Text(bin.displayName, color = Color.White) }, onClick = { onBinSelected(bin); expanded = false }) } } } }
