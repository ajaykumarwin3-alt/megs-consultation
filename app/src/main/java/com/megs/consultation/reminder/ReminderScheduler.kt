package com.megs.consultation.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.megs.consultation.MainActivity
import com.megs.consultation.data.Appointment
import com.megs.consultation.data.Status
import com.megs.consultation.data.TEST_ID
import com.megs.consultation.data.offsets
import com.megs.consultation.data.statusEnum
import com.megs.consultation.util.Fmt

/**
 * Schedules alarm-clock style reminders with AlarmManager so they fire even when
 * the app is closed / phone is in Doze. Slots 0..8 = configured reminders, slot 9 = snooze.
 */
object ReminderScheduler {
    const val EXTRA_ID = "appointment_id"
    const val EXTRA_LABEL = "label"
    const val EXTRA_MIN = "minutes"
    private const val ACTION_ALARM = "com.megs.consultation.ALARM"
    private const val SNOOZE_SLOT = 9

    private fun alarmPi(ctx: Context, id: Long, slot: Int, label: String?): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_LABEL, label)
        return PendingIntent.getBroadcast(
            ctx, requestCode(id, slot), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(id: Long, slot: Int): Int = ((id * 10 + slot) % Int.MAX_VALUE).toInt()

    fun schedule(ctx: Context, a: Appointment) {
        cancel(ctx, a.id)
        if (!a.reminderEnabled || a.statusEnum() != Status.UPCOMING) return
        val now = System.currentTimeMillis()
        a.offsets().take(SNOOZE_SLOT).forEachIndexed { slot, off ->
            val t = a.dateTime - off * 60_000L
            if (t > now) setAlarm(ctx, t, alarmPi(ctx, a.id, slot, Fmt.inLabel(off)))
        }
    }

    fun snooze(ctx: Context, id: Long, minutes: Int) {
        setAlarm(ctx, System.currentTimeMillis() + minutes * 60_000L,
            alarmPi(ctx, id, SNOOZE_SLOT, "Snoozed $minutes min"))
    }

    fun scheduleTest(ctx: Context, seconds: Int) {
        setAlarm(ctx, System.currentTimeMillis() + seconds * 1000L, alarmPi(ctx, TEST_ID, 0, "Test alarm"))
    }

    fun cancel(ctx: Context, id: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (slot in 0..SNOOZE_SLOT) am.cancel(alarmPi(ctx, id, slot, null))
    }

    fun nextTrigger(a: Appointment): Long? {
        if (!a.reminderEnabled || a.statusEnum() != Status.UPCOMING) return null
        val now = System.currentTimeMillis()
        return a.offsets().map { a.dateTime - it * 60_000L }.filter { it > now }.minOrNull()
    }

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)   // fallback (may be a few min late)
        } else {
            val show = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
        }
    }
}
