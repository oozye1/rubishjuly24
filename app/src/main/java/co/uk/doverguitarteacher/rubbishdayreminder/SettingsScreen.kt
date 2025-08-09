package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext

    var selectedDay by remember { mutableStateOf(settingsManager.getCollectionDay()) }
    var eveningReminder by remember { mutableStateOf(settingsManager.isEveningReminderEnabled()) }
    var morningReminder by remember { mutableStateOf(settingsManager.isMorningReminderEnabled()) }

    var eveningTime by remember { mutableStateOf(settingsManager.getEveningReminderTime()) }
    var morningTime by remember { mutableStateOf(settingsManager.getMorningReminderTime()) }

    data class PerBinState(
        var enabledOverrideDay: Boolean,
        var day: DayOfWeek,
        var frequency: Frequency,
        var anchorDate: LocalDate?
    )

    val perBinState = remember {
        BinTypes.ALL_BINS.associateWith { bin ->
            val customDay = settingsManager.getBinCollectionDay(bin.id)
            val effectiveDay = customDay ?: settingsManager.getCollectionDay()
            PerBinState(
                enabledOverrideDay = customDay != null,
                day = effectiveDay,
                frequency = settingsManager.getBinFrequency(bin.id),
                anchorDate = settingsManager.getBinAnchorDate(bin.id)
            )
        }.mapValues { mutableStateOf(it.value) }.toMutableMap()
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM yyyy") }
    val ctx = LocalContext.current

    fun showTimePicker(initial: LocalTime, onResult: (LocalTime) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onResult(LocalTime.of(h, m)) }, initial.hour, initial.minute, false).show()
    }

    fun showDatePicker(initial: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val base = initial ?: LocalDate.now()
        DatePickerDialog(ctx, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            base.year, base.monthValue - 1, base.dayOfMonth).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2c3e50))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))

        SettingRow(label = "Default Collection Day") {
            DayOfWeekSelector(selectedDay = selectedDay, onDaySelected = { selectedDay = it })
        }
        Spacer(Modifier.height(16.dp))

        Text("Per-Bin Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))

        BinTypes.ALL_BINS.forEach { bin ->
            val holder = perBinState[bin]!!
            val s = holder.value

            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = s.enabledOverrideDay,
                            onCheckedChange = { chk -> holder.value = s.copy(enabledOverrideDay = chk) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2ecc71), uncheckedColor = Color.LightGray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(bin.displayName, color = Color.White, fontSize = 16.sp)
                    }
                    if (s.enabledOverrideDay) {
                        Spacer(Modifier.width(16.dp))
                        DayOfWeekSelector(
                            selectedDay = s.day,
                            onDaySelected = { d -> holder.value = s.copy(day = d) },
                            modifier = Modifier.weight(1f).widthIn(min = 200.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Frequency", color = Color(0xFFbdc3c7), fontSize = 14.sp, modifier = Modifier.width(90.dp))
                    FrequencySelector(selected = s.frequency, onSelected = { f -> holder.value = s.copy(frequency = f) })
                    Spacer(Modifier.width(12.dp))
                    if (s.frequency == Frequency.FORTNIGHTLY) {
                        val label = s.anchorDate?.format(dateFormatter) ?: "Pick start date"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { showDatePicker(s.anchorDate) { picked -> holder.value = s.copy(anchorDate = picked) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text(label, color = Color.White, fontSize = 14.sp) }
                    }
                }
                Divider(Modifier.padding(top = 10.dp), color = Color(0xFF42576B))
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))

        ReminderRow(
            label = "Night Before Reminder",
            time = eveningTime,
            isChecked = eveningReminder,
            onCheckedChange = { eveningReminder = it },
            onTimeClicked = { if (eveningReminder) showTimePicker(eveningTime) { eveningTime = it } },
            enabled = eveningReminder,
            timeFormatter = timeFormatter
        )
        Spacer(Modifier.height(6.dp))
        ReminderRow(
            label = "Morning Reminder",
            time = morningTime,
            isChecked = morningReminder,
            onCheckedChange = { morningReminder = it },
            onTimeClicked = { if (morningReminder) showTimePicker(morningTime) { morningTime = it } },
            enabled = morningReminder,
            timeFormatter = timeFormatter
        )

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe74c3c))
            ) { Text("Cancel") }

            Button(
                onClick = {
                    // Save global default day + reminders
                    settingsManager.saveCollectionDay(selectedDay)
                    settingsManager.saveReminderSettings(
                        eveningEnabled = eveningReminder,
                        morningEnabled = morningReminder,
                        eveningTime = eveningTime,
                        morningTime = morningTime
                    )
                    // Save per-bin config
                    perBinState.forEach { (bin, holder) ->
                        val s = holder.value
                        settingsManager.saveBinCollectionDay(bin.id, if (s.enabledOverrideDay) s.day else null)
                        settingsManager.saveBinFrequency(bin.id, s.frequency)
                        settingsManager.saveBinAnchorDate(bin.id, if (s.frequency == Frequency.FORTNIGHTLY) s.anchorDate else null)
                    }
                    // Re-arm alarms (use hoisted appContext – avoids @Composable-in-non-composable error)
                    AlarmScheduler.scheduleAlarms(appContext, settingsManager)
                    onNavigateBack()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ecc71))
            ) { Text("Save") }
        }
    }
}

@Composable
fun ReminderRow(
    label: String,
    time: LocalTime,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTimeClicked: () -> Unit,
    enabled: Boolean,
    timeFormatter: DateTimeFormatter
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .clickable(enabled) { onTimeClicked() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = time.format(timeFormatter), color = if (enabled) Color.White else Color.Gray, fontSize = 14.sp)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2ecc71),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun SettingRow(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFFbdc3c7), fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
fun DayOfWeekSelector(selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Color.White, fontSize = 16.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select day", tint = Color.White)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF34495e))) {
            DayOfWeek.values().forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Color.White) },
                    onClick = { onDaySelected(day); expanded = false }
                )
            }
        }
    }
}

@Composable
fun FrequencySelector(selected: Frequency, onSelected: (Frequency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selected == Frequency.WEEKLY) "Weekly" else "Fortnightly"
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open", tint = Color.White)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF34495e))) {
            Frequency.values().forEach { f ->
                DropdownMenuItem(
                    text = { Text(if (f == Frequency.WEEKLY) "Weekly" else "Fortnightly", color = Color.White) },
                    onClick = { onSelected(f); expanded = false }
                )
            }
        }
    }
}
