package com.megs.consultation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megs.consultation.data.Status
import com.megs.consultation.data.statusEnum
import com.megs.consultation.ui.*
import com.megs.consultation.util.AppConfig
import com.megs.consultation.util.Fmt
import com.megs.consultation.util.TimeUtil

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onCalendar: () -> Unit,
    onSearch: () -> Unit
) {
    val all by vm.appointments.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val now = System.currentTimeMillis()
    val dayStart = TimeUtil.startOfDay(now)
    val dayEnd = TimeUtil.addDays(dayStart, 1)
    val weekEnd = TimeUtil.addDays(dayStart, 7)
    val monthStart = TimeUtil.startOfMonth(now)
    val monthEnd = TimeUtil.addMonths(monthStart, 1)

    val active = all.filter { it.statusEnum() != Status.CANCELLED }
    val today = active.filter { it.dateTime in dayStart until dayEnd }.sortedBy { it.dateTime }
    val todayFees = today.sumOf { it.fee ?: 0.0 }
    val upcomingWeek = all.count { it.statusEnum() == Status.UPCOMING && it.dateTime in now until weekEnd }
    val month = active.filter { it.dateTime in monthStart until monthEnd }
    val monthFees = month.sumOf { it.fee ?: 0.0 }
    val overdue = all.filter { it.statusEnum() == Status.UPCOMING && it.dateTime < dayStart }

    val list = when (tab) {
        0 -> today
        1 -> all.filter { it.statusEnum() == Status.UPCOMING && it.dateTime >= dayStart }.sortedBy { it.dateTime }
        else -> all.filter { it.statusEnum() == Status.COMPLETED }.sortedByDescending { it.dateTime }.take(50)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Green),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.EventAvailable, null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(AppConfig.APP_NAME, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(AppConfig.DOCTOR_NAME, color = Muted, fontSize = 13.sp)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search") }
                Box(Modifier.size(38.dp).clip(CircleShape).background(GreenLight),
                    contentAlignment = Alignment.Center) {
                    Text(AppConfig.DOCTOR_NAME.removePrefix("Dr. ").take(1), color = GreenDark,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Column {
                Text(Fmt.greeting(), fontSize = 18.sp, color = Color(0xFF3B4A41))
                Text(AppConfig.DOCTOR_NAME, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif, color = Color(0xFF13261C))
                Text("Here's your schedule for today.", color = Muted, fontSize = 14.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Outlined.Today, "Today", "${today.size}",
                    "appointment${if (today.size == 1) "" else "s"}", Modifier.weight(1f))
                StatCard(Icons.Outlined.Schedule, "Upcoming", "$upcomingWeek", "this week", Modifier.weight(1f))
                StatCard(Icons.Outlined.CurrencyRupee, "This Month", Fmt.rupee(monthFees),
                    "from ${month.size} consultations", Modifier.weight(1.25f))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GreenLight)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today: ${today.size} consultation${if (today.size == 1) "" else "s"}  •  ${Fmt.rupee(todayFees)}",
                    color = GreenDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("See Calendar ›", color = Green, fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onCalendar))
            }
        }
        if (overdue.isNotEmpty()) item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AmberSoft)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.WarningAmber, null, tint = Amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${overdue.size} past appointment${if (overdue.size == 1) "" else "s"} not marked completed",
                    color = Amber, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.markCompleted(overdue) }) { Text("Mark all", color = Amber) }
            }
        }
        item { SegmentedTabs(listOf("Today", "Upcoming", "Completed"), tab) { tab = it } }

        if (list.isEmpty()) item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyState(when (tab) {
                    0 -> "No appointments today"
                    1 -> "No upcoming appointments"
                    else -> "No completed consultations yet"
                })
                Button(onClick = onAdd) { Text("Add appointment") }
            }
        }
        items(list, key = { it.id }) { a -> AppointmentCard(a, vm, onEdit) }
    }
}
