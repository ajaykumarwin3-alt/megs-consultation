package com.megs.consultation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megs.consultation.data.Status
import com.megs.consultation.data.statusEnum
import com.megs.consultation.ui.*
import com.megs.consultation.util.Fmt
import com.megs.consultation.util.TimeUtil
import java.util.Calendar

@Composable
fun CalendarScreen(vm: MainViewModel, onAddOn: (Long) -> Unit, onEdit: (Long) -> Unit) {
    val all by vm.appointments.collectAsStateWithLifecycle()
    val today = TimeUtil.startOfDay(System.currentTimeMillis())
    var month by rememberSaveable { mutableLongStateOf(TimeUtil.startOfMonth(today)) }
    var selected by rememberSaveable { mutableLongStateOf(today) }

    val byDay = remember(all) {
        all.filter { it.statusEnum() != Status.CANCELLED }.groupBy { TimeUtil.startOfDay(it.dateTime) }
    }
    val monthEnd = TimeUtil.addMonths(month, 1)
    val monthItems = all.filter { it.dateTime in month until monthEnd && it.statusEnum() != Status.CANCELLED }
    val dayItems = all.filter { TimeUtil.startOfDay(it.dateTime) == selected }.sortedBy { it.dateTime }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenTitle("Calendar") {
                TextButton(onClick = { month = TimeUtil.startOfMonth(today); selected = today }) { Text("Today", color = Green) }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = TimeUtil.addMonths(month, -1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous") }
                        Text(Fmt.monthYear(month), fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                            modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = { month = TimeUtil.addMonths(month, 1) }) { Icon(Icons.Outlined.ChevronRight, "Next") }
                    }
                    Row {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                            Text(it, Modifier.weight(1f), color = Muted, fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val c = TimeUtil.cal(month)
                    val lead = c.get(Calendar.DAY_OF_WEEK) - 1
                    val days = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val cells: List<Long?> = List(lead) { null } +
                            (1..days).map { d -> TimeUtil.cal(month).apply { set(Calendar.DAY_OF_MONTH, d) }.timeInMillis }
                    cells.chunked(7).forEach { week ->
                        Row {
                            week.forEach { day ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    if (day != null) DayCell(day, day == selected, day == today, byDay[day]?.size ?: 0) { selected = day }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            Text("${Fmt.monthYear(month)}: ${monthItems.size} consultations  •  ${Fmt.rupee(monthItems.sumOf { it.fee ?: 0.0 })}",
                color = GreenDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.dayHeader(selected), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { onAddOn(selected) }) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
                }
            }
        }
        if (dayItems.isEmpty()) item { EmptyState("No appointments on this day") }
        items(dayItems, key = { it.id }) { AppointmentCard(it, vm, onEdit) }
    }
}

@Composable
private fun DayCell(day: Long, isSelected: Boolean, isToday: Boolean, count: Int, onClick: () -> Unit) {
    Column(
        Modifier.padding(2.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(if (isSelected) Green else Color.Transparent)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, Green, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(TimeUtil.cal(day).get(Calendar.DAY_OF_MONTH).toString(),
                color = if (isSelected) Color.White else Color(0xFF1E2A23),
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
        }
        Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(minOf(count, 3)) { Box(Modifier.size(5.dp).clip(CircleShape).background(Green)) }
        }
    }
}
