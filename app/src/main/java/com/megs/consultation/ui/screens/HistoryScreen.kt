package com.megs.consultation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megs.consultation.data.Status
import com.megs.consultation.data.statusEnum
import com.megs.consultation.ui.*
import com.megs.consultation.util.AppConfig
import com.megs.consultation.util.Contact
import com.megs.consultation.util.Fmt
import com.megs.consultation.util.TimeUtil

@Composable
fun HistoryScreen(vm: MainViewModel, onEdit: (Long) -> Unit) {
    val ctx = LocalContext.current
    val all by vm.appointments.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    var query by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableLongStateOf(TimeUtil.startOfMonth(now)) }
    var allTime by rememberSaveable { mutableStateOf(false) }
    var pay by rememberSaveable { mutableIntStateOf(0) }        // 0 all, 1 paid, 2 unpaid
    var placeFilter by rememberSaveable { mutableStateOf("") }
    var byName by rememberSaveable { mutableIntStateOf(0) }     // 0 by date, 1 by name

    val base = all.filter { it.statusEnum() != Status.CANCELLED && (it.statusEnum() == Status.COMPLETED || it.dateTime < now) }
    val places = remember(base) { base.map { it.place.trim() }.filter { it.isNotBlank() }.distinct().sorted() }
    val monthEnd = TimeUtil.addMonths(month, 1)
    val q = query.trim()
    val filtered = base.filter { a ->
        (allTime || a.dateTime in month until monthEnd) &&
            (q.isEmpty() || a.name.contains(q, true) || a.place.contains(q, true) || a.notes.contains(q, true) || a.type.contains(q, true)) &&
            (placeFilter.isEmpty() || a.place.trim() == placeFilter) &&
            when (pay) { 1 -> a.fee != null && a.paid; 2 -> a.fee != null && !a.paid; else -> true }
    }.sortedByDescending { it.dateTime }

    val total = filtered.sumOf { it.fee ?: 0.0 }
    val paidSum = filtered.filter { it.paid }.sumOf { it.fee ?: 0.0 }
    val unpaidSum = total - paidSum
    val periodLabel = if (allTime) "All time" else Fmt.monthYear(month)

    fun shareSummary() {
        val sb = StringBuilder("${AppConfig.APP_NAME} – $periodLabel\n\n")
        filtered.sortedBy { it.dateTime }.forEach { a ->
            sb.append("${Fmt.dateShort(a.dateTime)} • ${a.name}")
            if (a.fee != null) sb.append(" – ${Fmt.rupee(a.fee)}${if (a.paid) " ✓" else ""}")
            sb.append("\n")
        }
        sb.append("\nTotal: ${Fmt.rupee(total)} (${filtered.size} consultations)")
        if (unpaidSum > 0) sb.append("\nPending: ${Fmt.rupee(unpaidSum)}")
        Contact.shareText(ctx, sb.toString())
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ScreenTitle("History") {
                IconButton(onClick = ::shareSummary) { Icon(Icons.Outlined.Share, "Share summary", tint = Green) }
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search name, place, notes") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFDCE3DE),
                    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { allTime = false; month = TimeUtil.addMonths(month, -1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                Text(periodLabel, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { allTime = false; month = TimeUtil.addMonths(month, 1) }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
                TextButton(onClick = { allTime = !allTime }) { Text(if (allTime) "By month" else "All time", color = Green) }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill("All", pay == 0) { pay = 0 }
                FilterPill("Paid", pay == 1) { pay = 1 }
                FilterPill("Unpaid", pay == 2) { pay = 2 }
                if (places.size > 1) {
                    Spacer(Modifier.width(6.dp))
                    FilterPill("All places", placeFilter.isEmpty()) { placeFilter = "" }
                    places.forEach { p -> FilterPill(p, placeFilter == p) { placeFilter = if (placeFilter == p) "" else p } }
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Green, GreenDark))).padding(16.dp)
            ) {
                Text("Total • $periodLabel", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(Fmt.rupee(total), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} consultation${if (filtered.size == 1) "" else "s"}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Paid ${Fmt.rupee(paidSum)}", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("Unpaid ${Fmt.rupee(unpaidSum)}", color = Color(0xFFFFE3A8), fontSize = 13.sp)
                }
            }
        }
        item { SegmentedTabs(listOf("By date", "By name"), byName) { byName = it } }

        if (filtered.isEmpty()) item { EmptyState("No consultations found") }
        else if (byName == 0) {
            filtered.groupBy { TimeUtil.startOfDay(it.dateTime) }.forEach { (day, list) ->
                item(key = "h$day") {
                    Row(Modifier.padding(top = 6.dp)) {
                        Text(Fmt.dayHeader(day), fontWeight = FontWeight.SemiBold, color = Color(0xFF34423A), modifier = Modifier.weight(1f))
                        Text(Fmt.rupee(list.sumOf { it.fee ?: 0.0 }), fontWeight = FontWeight.SemiBold, color = GreenDark)
                    }
                }
                items(list, key = { it.id }) { AppointmentCard(it, vm, onEdit, showActions = false) }
            }
        } else {
            val grouped = filtered.groupBy { it.name.trim() }
                .map { (n, l) -> Triple(n, l.sumOf { it.fee ?: 0.0 }, l.size) }
                .sortedByDescending { it.second }
            items(grouped, key = { "n" + it.first }) { (n, sum, count) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(n, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count visit${if (count == 1) "" else "s"}", fontSize = 12.sp, color = Muted)
                    }
                    Text(Fmt.rupee(sum), fontWeight = FontWeight.Bold, color = GreenDark)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) =
    SelectChip(text, selected, onClick, Modifier.height(40.dp))
