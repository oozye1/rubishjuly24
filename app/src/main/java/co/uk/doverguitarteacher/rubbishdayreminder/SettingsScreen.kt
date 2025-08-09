package co.uk.doverguitarteacher.rubbishdayreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM") }
    val ctx = LocalContext.current

    fun showTimePicker(initial: LocalTime, onResult: (LocalTime) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onResult(LocalTime.of(h, m)) }, initial.hour, initial.minute, false).show()
    }

    fun showDatePicker(initial: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val base = initial ?: LocalDate.now()
        DatePickerDialog(ctx, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            base.year, base.monthValue - 1, base.dayOfMonth).show()
    }

    Scaffold(
        containerColor = Color(0xFF2c3e50),
        bottomBar = {
            Surface(tonalElevation = 2.dp, color = Color(0xFF223242)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe74c3c))
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            settingsManager.saveCollectionDay(selectedDay)
                            settingsManager.saveReminderSettings(
                                eveningEnabled = eveningReminder,
                                morningEnabled = morningReminder,
                                eveningTime = eveningTime,
                                morningTime = morningTime
                            )
                            perBinState.forEach { (bin, holder) ->
                                val s = holder.value
                                settingsManager.saveBinCollectionDay(bin.id, if (s.enabledOverrideDay) s.day else null)
                                settingsManager.saveBinFrequency(bin.id, s.frequency)
                                settingsManager.saveBinAnchorDate(
                                    bin.id,
                                    if (s.frequency == Frequency.FORTNIGHTLY) s.anchorDate else null
                                )
                            }
                            AlarmScheduler.scheduleAlarms(appContext, settingsManager)
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ecc71))
                    ) { Text("Save") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 12.dp)
        ) {
            item {
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Default collection day", color = Color(0xFFbdc3c7), fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        DayOfWeekCompactDropdown(selectedDay) { selectedDay = it }
                    }
                }
            }

            item {
                Text("Per-bin settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(BinTypes.ALL_BINS) { bin ->
                val holder = perBinState[bin]!!
                val s = holder.value
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(bin.displayName, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        CompactSwitchLabel(
                            label = "Override day",
                            checked = s.enabledOverrideDay,
                            onCheckedChange = { holder.value = s.copy(enabledOverrideDay = it) }
                        )
                    }
                    if (s.enabledOverrideDay) {
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Day", color = Color(0xFFbdc3c7), fontSize = 12.sp)
                            Spacer(Modifier.width(10.dp))
                            DayOfWeekCompactDropdown(s.day) { d -> holder.value = s.copy(day = d) }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Frequency", color = Color(0xFFbdc3c7), fontSize = 12.sp)
                        Spacer(Modifier.width(10.dp))
                        FrequencyCompactDropdown(s.frequency) { f -> holder.value = s.copy(frequency = f) }
                        if (s.frequency == Frequency.FORTNIGHTLY) {
                            Spacer(Modifier.width(10.dp))
                            val label = s.anchorDate?.format(dateFormatter) ?: "Start date"
                            CompactChip(label) {
                                showDatePicker(s.anchorDate) { picked -> holder.value = s.copy(anchorDate = picked) }
                            }
                        }
                    }
                }
            }

            item {
                Text("Reminders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                SectionCard {
                    CompactReminderRow(
                        label = "Night before",
                        time = eveningTime,
                        isChecked = eveningReminder,
                        onCheckedChange = { eveningReminder = it },
                        onPickTime = { if (eveningReminder) showTimePicker(eveningTime) { eveningTime = it } },
                        timeFormatter = timeFormatter
                    )
                    Divider(color = Color(0xFF3A5166))
                    CompactReminderRow(
                        label = "Morning",
                        time = morningTime,
                        isChecked = morningReminder,
                        onCheckedChange = { morningReminder = it },
                        onPickTime = { if (morningReminder) showTimePicker(morningTime) { morningTime = it } },
                        timeFormatter = timeFormatter
                    )
                }
            }
        }
    }
}

/* ---------- Material3 Card-based compact UI ---------- */

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF263645)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            content = content
        )
    }
}

@Composable
private fun CompactChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF4C657C), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun CompactSwitchLabel(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFbdc3c7), fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(18.dp),
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
private fun DropdownBox(currentText: String, items: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF4C657C), RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currentText, color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF34495e)
        ) {
            items.forEachIndexed { index, txt ->
                DropdownMenuItem(
                    text = { Text(txt, color = Color.White, fontSize = 13.sp) },
                    onClick = { onSelect(index); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekCompactDropdown(selected: DayOfWeek, onSelected: (DayOfWeek) -> Unit) {
    val names = remember {
        DayOfWeek.values().map { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    }
    DropdownBox(
        currentText = selected.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        items = names
    ) { idx -> onSelected(DayOfWeek.values()[idx]) }
}

@Composable
private fun FrequencyCompactDropdown(selected: Frequency, onSelected: (Frequency) -> Unit) {
    val items = listOf("Weekly", "Fortnightly")
    val current = if (selected == Frequency.WEEKLY) "Weekly" else "Fortnightly"
    DropdownBox(currentText = current, items = items) { idx ->
        onSelected(if (idx == 0) Frequency.WEEKLY else Frequency.FORTNIGHTLY)
    }
}

@Composable
private fun CompactReminderRow(
    label: String,
    time: LocalTime,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    timeFormatter: DateTimeFormatter
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        CompactChip(time.format(timeFormatter)) { if (isChecked) onPickTime() }
        Spacer(Modifier.width(8.dp))
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
