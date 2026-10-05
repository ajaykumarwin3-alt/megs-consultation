package com.megs.consultation.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.megs.consultation.data.AppDatabase
import com.megs.consultation.data.Status
import com.megs.consultation.data.TEST_ID
import com.megs.consultation.data.demoAppointment
import com.megs.consultation.data.statusEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fired by AlarmManager at the reminder time. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0L)
        val label = intent.getStringExtra(ReminderScheduler.EXTRA_LABEL)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val a = if (id == TEST_ID) demoAppointment() else AppDatabase.get(ctx).dao().get(id)
                if (a != null && (id == TEST_ID || a.statusEnum() == Status.UPCOMING)) {
                    NotificationHelper.showAlarm(ctx, a, label)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Snooze / Dismiss buttons on the notification. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SNOOZE = "com.megs.consultation.SNOOZE"
        const val ACTION_DISMISS = "com.megs.consultation.DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0L)
        NotificationHelper.cancel(context, id)
        if (intent.action == ACTION_SNOOZE) {
            val min = intent.getIntExtra(ReminderScheduler.EXTRA_MIN, 10)
            if (id == TEST_ID) ReminderScheduler.scheduleTest(context, min * 60)
            else ReminderScheduler.snooze(context, id, min)
        }
    }
}

/** Alarms are cleared on reboot / app update – re-register them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(ctx).dao().upcomingAfter(System.currentTimeMillis())
                    .forEach { ReminderScheduler.schedule(ctx, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
