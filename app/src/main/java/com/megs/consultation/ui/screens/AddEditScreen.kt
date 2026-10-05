package com.megs.consultation.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megs.consultation.data.Appointment
import com.megs.consultation.data.Status
import com.megs.consultation.ui.*
import com.megs.consultation.util.AppConfig
import com.megs.consultation.util.Fmt
import com.megs.consultation.util.TimeUtil
import java.util.Calendar

private val PRESET_REMINDERS = listOf(1440 to "1 day\nbefore", 60 to "1 hour\nbefore", 30 to "30 min\nbefore", 0 to "At\ntime")

@Composable
fun AddEditScreen(vm: MainViewModel, id: Long, presetDate: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val all by vm.appointments.collectAsStateWithLifecycle()

    var loaded by rememberSaveable { mutableStateOf(false) }
    var existing by remember { mutableStateOf<Appointment?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Consultation") }
    var phone by rememberSaveable { mutableStateOf("") }
    var whenMs by rememberSaveable { mutableLongStateOf(TimeUtil.defaultSlot(presetDate)) }
    var place by rememberSaveable { mutableStateOf("") }
    var feeText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var paid by rememberSaveable { mutableStateOf(false) }
    var remindersOn by rememberSaveable { mutableStateOf(true) }
    var offsetsCsv by rememberSaveable { mutableStateOf("60") }
    var sound by rememberSaveable { mutableStateOf(true) }
    var nameError by remember { mutableStateOf(false) }
    var placeError by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }

    val offsets = offsetsCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    fun toggleOffset(m: Int) {
        val s = offsets.toMutableSet(); if (!s.add(m)) s.remove(m)
        offsetsCsv = s.sortedDescending().joinToString(",")
    }

    // Load existing appointment once (edit mode)
    LaunchedEffect(id) {
        if (id != 0L) {
            vm.get(id)?.let { a ->
                existing = a
                if (!loaded) {
                    name = a.name; type = a.type; phone = a.phone; whenMs = a.dateTime; place = a.place
                    feeText = a.fee?.let { Fmt.amount(it).replace(",", "") } ?: ""
                    notes = a.notes; paid = a.paid; remindersOn = a.reminderEnabled
                    offsetsCsv = a.reminderOffsets; sound = a.soundAlert
                    showMore = a.phone.isNotBlank()
                }
            }
        }
        loaded = true
    }
    // New entry: default place = most recently used place
    LaunchedEffect(all.isNotEmpty()) {
        if (id == 0L && place.isBlank()) all.maxByOrNull { it.createdAt }?.let { place = it.place }
    }

    val recentPlaces = remember(all) {
        all.map { it.place.trim() }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6).map { it.key }
    }
    val nameSuggestions = remember(name, all) {
        if (name.isBlank() || id != 0L) emptyList()
        else all.map { it.name.trim() }.distinct()
            .filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }
            .take(5)
    }

    fun pickDate() {
        val c = TimeUtil.cal(whenMs)
        DatePickerDialog(ctx, { _, y, m, d ->
            whenMs = TimeUtil.cal(whenMs).apply { set(y, m, d) }.timeInMillis
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun pickTime() {
        val c = TimeUtil.cal(whenMs)
        TimePickerDialog(ctx, { _, h, min ->
            whenMs = TimeUtil.cal(whenMs).apply {
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }

    fun save() {
        nameError = name.isBlank(); placeError = place.isBlank()
        if (nameError || placeError) return
        val fee = feeText.replace(",", "").trim().toDoubleOrNull()
        var status = existing?.status ?: Status.UPCOMING.name
        val isPastNew = existing == null && whenMs < System.currentTimeMillis()
        if (isPastNew) status = Status.COMPLETED.name     // quick logging of an already-done consultation
        val base = existing ?: Appointment(name = "", dateTime = 0, place = "")
        val a = base.copy(
            name = name.trim(), type = type.trim(), phone = phone.trim(), dateTime = whenMs,
            place = place.trim(), fee = fee, notes = notes.trim(), paid = paid,
            reminderEnabled = remindersOn && offsets.isNotEmpty(), reminderOffsets = offsetsCsv,
            soundAlert = sound, status = status
        )
        vm.save(a) {
            Toast.makeText(ctx,
                if (isPastNew) "Saved as completed consultation" else "Appointment saved",
                Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(if (id == 0L) "Add Appointment" else "Edit Appointment",
                fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            existing?.let { ex ->
                IconButton(onClick = { vm.delete(ex); onBack() }) { Icon(Icons.Outlined.Delete, "Delete", tint = Red) }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Section {
                Label("Patient / Doctor / Clinic Name", required = true)
                InputField(name, { name = it; nameError = false }, "e.g. Dr. Athila, Ekaa",
                    Icons.Outlined.Person, isError = nameError, capitalize = true)
                if (nameSuggestions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nameSuggestions.forEach { s ->
                            SuggestionPill(s) {
                                name = s
                                all.filter { it.name.trim() == s }.maxByOrNull { it.dateTime }?.let { last ->
                                    place = last.place; type = last.type
                                    if (phone.isBlank()) phone = last.phone
                                    if (feeText.isBlank()) feeText = last.fee?.let { Fmt.amount(it).replace(",", "") } ?: ""
                                }
                            }
                        }
                    }
                }

                Label("Type")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppConfig.TYPES.forEach { t -> SuggestionPill(t, selected = type == t) { type = t } }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1.3f)) {
                        Label("Date", required = true)
                        PickerBox(Icons.Outlined.CalendarMonth, Fmt.date(whenMs), ::pickDate)
                    }
                    Column(Modifier.weight(1f)) {
                        Label("Time", required = true)
                        PickerBox(Icons.Outlined.Schedule, Fmt.time(whenMs), ::pickTime)
                    }
                }

                Label("Place / Clinic", required = true)
                InputField(place, { place = it; placeError = false }, "e.g. Clinic, Hormavu",
                    Icons.Outlined.Place, isError = placeError, capitalize = true)
                if (recentPlaces.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recentPlaces.forEach { p -> SuggestionPill(p, selected = place == p) { place = p; placeError = false } }
                    }
                }

                Label("Consultation Fee (Optional)")
                InputField(feeText, { v -> feeText = v.filter { it.isDigit() || it == '.' } }, "Amount",
                    Icons.Outlined.CurrencyRupee, keyboard = KeyboardType.Number)
                if (feeText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = paid, onCheckedChange = { paid = it })
                        Text("Payment received", fontSize = 14.sp)
                    }
                }

                Label("Notes (Optional)")
                OutlinedTextField(
                    value = notes, onValueChange = { if (it.length <= 200) notes = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    placeholder = { Text("Follow up, procedure details…") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) },
                    supportingText = { Text("${notes.length}/200", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )

                Text(
                    if (showMore) "− Hide contact" else "+ Add phone number (for Call / WhatsApp)",
                    color = Green, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showMore = !showMore }
                )
                if (showMore) InputField(phone, { phone = it }, "Phone number", Icons.Outlined.Phone, keyboard = KeyboardType.Phone)
            }

            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Get notified before the appointment", color = Muted, fontSize = 12.sp)
                    }
                    Switch(checked = remindersOn, onCheckedChange = { remindersOn = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green))
                }
                if (remindersOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRESET_REMINDERS.forEach { (m, label) ->
                            SelectChip(label, m in offsets, { toggleOffset(m) }, Modifier.weight(1f))
                        }
                        SelectChip("Custom\n+", false, { showCustom = true }, Modifier.weight(1f))
                    }
                    val custom = offsets.filter { o -> PRESET_REMINDERS.none { it.first == o } }
                    if (custom.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            custom.sortedDescending().forEach { m -> SuggestionPill("${Fmt.offsetLabel(m)}  ✕", selected = true) { toggleOffset(m) } }
                        }
                    }
                    Text("Tip: select more than one for important appointments.", color = Muted, fontSize = 12.sp)
                    Label("Alert Type")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("Sound + Notification", sound, { sound = true }, Modifier.weight(1f), Icons.Outlined.Alarm)
                        SelectChip("Notification only", !sound, { sound = false }, Modifier.weight(1f), Icons.Outlined.Notifications)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Button(
            onClick = ::save,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) { Text(if (id == 0L) "Save Appointment" else "Update Appointment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }

    if (showCustom) CustomReminderDialog(onDismiss = { showCustom = false }) { m ->
        if (m !in offsets) toggleOffset(m); showCustom = false
    }
}

@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun Label(text: String, required: Boolean = false) {
    Row {
        Text(text, fontSize = 14.sp, color = Color(0xFF34423A), fontWeight = FontWeight.Medium)
        if (required) Text(" *", color = Red, fontSize = 14.sp)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFDCE3DE),
    focusedLeadingIconColor = Green
)

@Composable
private fun InputField(
    value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector,
    isError: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, capitalize: Boolean = false
) {
    val support: (@Composable () -> Unit)? = if (isError) { { Text("Required") } } else null
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Clear") }
        },
        isError = isError,
        supportingText = support,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None
        ),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors()
    )
}

@Composable
private fun PickerBox(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFDCE3DE), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Icon(Icons.Outlined.ArrowDropDown, null, tint = Muted)
    }
}

@Composable
private fun SuggestionPill(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text, fontSize = 13.sp,
        color = if (selected) Color.White else GreenDark,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) Green else GreenLight)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var amount by remember { mutableStateOf("2") }
    var unit by remember { mutableIntStateOf(1) } // 0=min 1=hours 2=days
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { v -> amount = v.filter { it.isDigit() }.take(3) },
                    singleLine = true, label = { Text("How long before") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SegmentedTabs(listOf("Minutes", "Hours", "Days"), unit) { unit = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = amount.toIntOrNull() ?: 0
                if (n > 0) onAdd(n * when (unit) { 0 -> 1; 1 -> 60; else -> 1440 })
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
