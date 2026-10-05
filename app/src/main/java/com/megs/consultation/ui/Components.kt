package com.megs.consultation.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.megs.consultation.data.Appointment
import com.megs.consultation.data.Status
import com.megs.consultation.data.statusEnum
import com.megs.consultation.util.Contact
import com.megs.consultation.util.Fmt

@Composable
fun IconText(icon: ImageVector, text: String, color: Color = Muted) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DateBlock(ms: Long) {
    Column(
        Modifier.width(54.dp).clip(RoundedCornerShape(12.dp)).background(GreenLight).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Fmt.dd(ms), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenDark)
        Text(Fmt.mon(ms), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreenDark)
        Text(Fmt.dowShort(ms), fontSize = 11.sp, color = Muted)
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, icon: ImageVector? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
        }
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatusPill(a: Appointment) = when (a.statusEnum()) {
    Status.UPCOMING ->
        if (a.reminderEnabled) Pill("Reminder On", GreenLight, GreenDark, Icons.Outlined.NotificationsActive)
        else Pill("Reminder Off", SlateSoft, Slate, Icons.Outlined.NotificationsOff)
    Status.COMPLETED ->
        if (a.fee == null) Pill("Completed", GreenLight, GreenDark, Icons.Outlined.CheckCircle)
        else if (a.paid) Pill("Paid", GreenLight, GreenDark, Icons.Outlined.CheckCircle)
        else Pill("Unpaid", AmberSoft, Amber, Icons.Outlined.Schedule)
    Status.CANCELLED -> Pill("Cancelled", RedSoft, Red, Icons.Outlined.Cancel)
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(4.dp)
    ) {
        options.forEachIndexed { i, s ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Green else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(s, color = if (sel) Color.White else Muted, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun SelectChip(
    label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, icon: ImageVector? = null
) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) GreenLight else Color.White)
            .border(1.dp, if (selected) Green else Color(0xFFDCE3DE), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Green else Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
        }
        Text(label, fontSize = 13.sp, textAlign = TextAlign.Center,
            color = if (selected) GreenDark else Color(0xFF26332B),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun StatCard(icon: ImageVector, title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(GreenLight),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Green, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Muted)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(sub, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(GreenLight).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Green, modifier = Modifier.size(19.dp)) }
}

@Composable
fun AppointmentCard(a: Appointment, vm: MainViewModel, onEdit: (Long) -> Unit, showActions: Boolean = true) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val status = a.statusEnum()

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onEdit(a.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                DateBlock(a.dateTime)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        a.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (status == Status.CANCELLED) TextDecoration.LineThrough else null
                    )
                    if (a.type.isNotBlank()) Text(a.type, fontSize = 12.sp, color = Green)
                    IconText(Icons.Outlined.Schedule, Fmt.time(a.dateTime))
                    if (a.place.isNotBlank()) IconText(Icons.Outlined.Place, a.place)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (a.fee != null) Text(Fmt.rupee(a.fee), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Box {
                            Icon(
                                Icons.Outlined.MoreVert, "More",
                                Modifier.size(30.dp).clip(CircleShape).clickable { menu = true }.padding(5.dp),
                                tint = Muted
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(a.id) })
                                if (status != Status.COMPLETED) DropdownMenuItem(
                                    text = { Text("Mark completed") },
                                    onClick = { menu = false; vm.setStatus(a, Status.COMPLETED) })
                                if (status != Status.UPCOMING) DropdownMenuItem(
                                    text = { Text("Mark upcoming") },
                                    onClick = { menu = false; vm.setStatus(a, Status.UPCOMING) })
                                if (a.fee != null) DropdownMenuItem(
                                    text = { Text(if (a.paid) "Mark unpaid" else "Mark paid") },
                                    onClick = { menu = false; vm.togglePaid(a) })
                                if (status != Status.CANCELLED) DropdownMenuItem(
                                    text = { Text("Cancel appointment") },
                                    onClick = { menu = false; vm.setStatus(a, Status.CANCELLED) })
                                DropdownMenuItem(text = { Text("Delete", color = Red) },
                                    onClick = { menu = false; confirmDelete = true })
                            }
                        }
                    }
                    StatusPill(a)
                }
            }
            if (showActions && (status == Status.UPCOMING || a.phone.isNotBlank())) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (status == Status.UPCOMING) {
                        SmallAction(Icons.Outlined.Snooze, "Snooze", GreenLight, GreenDark, Modifier.weight(1f)) {
                            vm.snooze(a, 10)
                            Toast.makeText(ctx, "Will remind you again in 10 min", Toast.LENGTH_SHORT).show()
                        }
                        SmallAction(
                            if (a.reminderEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                            if (a.reminderEnabled) "Off" else "On", SlateSoft, Slate, Modifier.weight(1f)
                        ) { vm.setReminder(a, !a.reminderEnabled) }
                    } else Spacer(Modifier.weight(1f))
                    if (a.phone.isNotBlank()) {
                        RoundIcon(Icons.Outlined.Call, "Call") { Contact.dial(ctx, a.phone) }
                        RoundIcon(Icons.AutoMirrored.Outlined.Chat, "WhatsApp") {
                            Contact.whatsapp(ctx, a.phone,
                                "Hello, this is a reminder for the appointment on ${Fmt.date(a.dateTime)} at " +
                                        "${Fmt.time(a.dateTime)}${if (a.place.isNotBlank()) ", ${a.place}" else ""}.")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete appointment?") },
        text = { Text("${a.name} on ${Fmt.date(a.dateTime)} will be permanently removed.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(a) }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
    )
}

@Composable
fun EmptyState(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.EventAvailable, null, tint = Color(0xFFB9C9BF), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun ScreenTitle(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}
