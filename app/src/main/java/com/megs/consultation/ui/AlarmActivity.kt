package com.megs.consultation.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.megs.consultation.data.AppDatabase
import com.megs.consultation.data.Appointment
import com.megs.consultation.data.TEST_ID
import com.megs.consultation.data.demoAppointment
import com.megs.consultation.reminder.NotificationHelper
import com.megs.consultation.reminder.ReminderScheduler
import com.megs.consultation.util.AppConfig
import com.megs.consultation.util.Fmt

/** Full-screen alarm shown over the lock screen with Snooze / Dismiss. */
class AlarmActivity : ComponentActivity() {
    private var apptId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        render(intent)
    }

    private fun render(i: Intent) {
        apptId = i.getLongExtra(ReminderScheduler.EXTRA_ID, 0L)
        val label = i.getStringExtra(ReminderScheduler.EXTRA_LABEL)
        val id = apptId
        setContent {
            MegsTheme {
                AlarmScreen(id, label, onSnooze = { act(it) }, onDismiss = { act(null) })
            }
        }
    }

    private fun act(snoozeMin: Int?) {
        NotificationHelper.cancel(this, apptId)
        if (snoozeMin != null) {
            if (apptId == TEST_ID) ReminderScheduler.scheduleTest(this, snoozeMin * 60)
            else ReminderScheduler.snooze(this, apptId, snoozeMin)
            Toast.makeText(this, "Snoozed for $snoozeMin min", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}

@Composable
private fun AlarmScreen(id: Long, label: String?, onSnooze: (Int) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val appt by produceState<Appointment?>(null, id) {
        value = if (id == TEST_ID) demoAppointment() else AppDatabase.get(ctx).dao().get(id)
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(1f, 1.12f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFDDF1E4), Color(0xFFF4FAF6))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.EventAvailable, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(AppConfig.APP_NAME, fontWeight = FontWeight.SemiBold)
                Text(AppConfig.DOCTOR_NAME, fontSize = 12.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(140.dp).scale(scale).clip(CircleShape).background(Green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Notifications, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Appointment Reminder", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label ?: "It's time for your consultation", color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))

        appt?.let { a ->
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(GreenLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.LocalHospital, null, tint = Green)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(a.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            if (a.type.isNotBlank()) Text(a.type, color = Green, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE8EEEA))
                    DetailRow(Icons.Outlined.CalendarMonth, Fmt.date(a.dateTime), Fmt.dow(a.dateTime))
                    DetailRow(Icons.Outlined.Schedule, Fmt.time(a.dateTime))
                    if (a.place.isNotBlank()) DetailRow(Icons.Outlined.Place, a.place)
                    if (a.fee != null) DetailRow(Icons.Outlined.CurrencyRupee, Fmt.rupee(a.fee))
                    if (a.notes.isNotBlank()) DetailRow(Icons.AutoMirrored.Outlined.Notes, a.notes)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = { onSnooze(10) }, modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
            Icon(Icons.Outlined.Snooze, null); Spacer(Modifier.width(10.dp))
            Text("Snooze", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = RedSoft, contentColor = Red)) {
            Icon(Icons.Filled.Cancel, null, tint = Red); Spacer(Modifier.width(10.dp))
            Text("Dismiss", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(18.dp))
        Text("Snooze for", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { m ->
                Text("$m min", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(Color.White)
                        .clickable { onSnooze(m) }.padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF34423A), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (trailing != null) Text("($trailing)", color = Muted, fontSize = 14.sp)
    }
}
