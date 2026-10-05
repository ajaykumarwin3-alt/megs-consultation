package com.megs.consultation.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megs.consultation.data.Status
import com.megs.consultation.data.offsets
import com.megs.consultation.data.statusEnum
import com.megs.consultation.reminder.ReminderScheduler
import com.megs.consultation.ui.*
import com.megs.consultation.util.Fmt

private data class SetupIssue(val title: String, val desc: String, val intent: Intent)

private fun setupIssues(ctx: Context): List<SetupIssue> = buildList {
    val pkg = ctx.packageName
    if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) add(SetupIssue(
        "Notifications are off", "Reminders cannot be shown until notifications are allowed.",
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) add(SetupIssue(
        "Allow exact alarms", "Needed so reminders ring exactly on time.",
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg"))))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) add(SetupIssue(
        "Allow full-screen alerts", "Shows the alarm screen with Snooze / Dismiss on the lock screen.",
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg"))))
    if (!ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg)) add(SetupIssue(
        "Recommended: disable battery optimisation", "Some phones (Xiaomi, Oppo, Vivo, Samsung) delay alarms. Set this app to 'Don't optimise / Unrestricted'.",
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))
}

@Composable
fun AlertsScreen(vm: MainViewModel, onEdit: (Long) -> Unit) {
    val ctx = LocalContext.current
    val all by vm.appointments.collectAsStateWithLifecycle()

    // Re-check permissions every time the screen resumes (user may return from Settings)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val issues = remember(tick) { setupIssues(ctx) }

    val now = System.currentTimeMillis()
    val upcoming = all.filter { it.statusEnum() == Status.UPCOMING && it.dateTime > now }.sortedBy { it.dateTime }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Alerts") }
        items(issues) { issue ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AmberSoft)
                    .clickable {
                        try { ctx.startActivity(issue.intent) } catch (e: Exception) {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                        }
                    }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.WarningAmber, null, tint = Amber)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(issue.title, fontWeight = FontWeight.SemiBold, color = Amber)
                    Text(issue.desc, fontSize = 12.sp, color = Color(0xFF6E5420))
                }
                Text("Fix ›", color = Amber, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            OutlinedButton(onClick = {
                ReminderScheduler.scheduleTest(ctx, 5)
                Toast.makeText(ctx, "Test alarm in 5 seconds – lock the screen to see the full-screen alert", Toast.LENGTH_LONG).show()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Alarm, null); Spacer(Modifier.width(8.dp)); Text("Test reminder (5 sec)")
            }
        }
        item { Text("Scheduled reminders", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) }
        if (upcoming.isEmpty()) item { EmptyState("No upcoming appointments") }
        items(upcoming, key = { it.id }) { a ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onEdit(a.id) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    DateBlock(a.dateTime)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(a.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        IconText(Icons.Outlined.Schedule, "${Fmt.time(a.dateTime)}  •  ${a.place}")
                        if (a.reminderEnabled) {
                            Text(a.offsets().joinToString(", ") { Fmt.offsetLabel(it) }, fontSize = 12.sp, color = Green)
                            val next = ReminderScheduler.nextTrigger(a)
                            Text(if (next != null) "Next alert: ${Fmt.dateTime(next)}" else "All reminder times have passed",
                                fontSize = 12.sp, color = Muted)
                        } else Text("Reminder off", fontSize = 12.sp, color = Muted)
                    }
                    Switch(checked = a.reminderEnabled, onCheckedChange = { vm.setReminder(a, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green))
                }
            }
        }
    }
}
