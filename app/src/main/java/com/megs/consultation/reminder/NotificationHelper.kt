package com.megs.consultation.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.megs.consultation.R
import com.megs.consultation.data.Appointment
import com.megs.consultation.ui.AlarmActivity
import com.megs.consultation.util.Fmt

object NotificationHelper {
    const val CH_ALARM = "appt_alarm_v1"
    const val CH_QUIET = "appt_quiet_v1"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(CH_ALARM, "Appointment alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm-style reminders with sound and full-screen alert"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            setSound(sound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val quiet = NotificationChannel(CH_QUIET, "Appointment reminders (quiet)", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notification-only reminders"
            setSound(null, null)
        }
        nm.createNotificationChannels(listOf(alarm, quiet))
    }

    fun notifId(id: Long): Int = ((id + 1000) % Int.MAX_VALUE).toInt()

    @SuppressLint("MissingPermission")
    fun showAlarm(ctx: Context, a: Appointment, label: String?) {
        createChannels(ctx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val full = Intent(ctx, AlarmActivity::class.java)
            .putExtra(ReminderScheduler.EXTRA_ID, a.id)
            .putExtra(ReminderScheduler.EXTRA_LABEL, label)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val fullPi = PendingIntent.getActivity(ctx, notifId(a.id), full, flags)

        fun action(act: String, code: Int) = PendingIntent.getBroadcast(
            ctx, notifId(a.id) * 3 + code,
            Intent(ctx, ActionReceiver::class.java).setAction(act)
                .putExtra(ReminderScheduler.EXTRA_ID, a.id)
                .putExtra(ReminderScheduler.EXTRA_MIN, 10),
            flags
        )

        val line = listOfNotNull(
            Fmt.time(a.dateTime), a.place.ifBlank { null }, a.fee?.let { Fmt.rupee(it) }
        ).joinToString("  •  ")
        val big = if (a.notes.isBlank()) line else "$line\n${a.notes}"

        val b = NotificationCompat.Builder(ctx, if (a.soundAlert) CH_ALARM else CH_QUIET)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1B8A5A.toInt())
            .setContentTitle(a.name)
            .setContentText(line)
            .setSubText(label)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setCategory(if (a.soundAlert) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullPi)
            .setAutoCancel(true)
            .addAction(0, "Snooze 10 min", action(ActionReceiver.ACTION_SNOOZE, 1))
            .addAction(0, "Dismiss", action(ActionReceiver.ACTION_DISMISS, 2))

        if (a.soundAlert) b.setFullScreenIntent(fullPi, true).setOngoing(true)

        val n = b.build()
        if (a.soundAlert) n.flags = n.flags or Notification.FLAG_INSISTENT   // keep ringing until acted on
        NotificationManagerCompat.from(ctx).notify(notifId(a.id), n)
    }

    fun cancel(ctx: Context, id: Long) = NotificationManagerCompat.from(ctx).cancel(notifId(id))
}
