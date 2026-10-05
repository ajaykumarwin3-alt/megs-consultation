package com.megs.consultation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.megs.consultation.data.AppDatabase
import com.megs.consultation.data.Appointment
import com.megs.consultation.data.Status
import com.megs.consultation.reminder.NotificationHelper
import com.megs.consultation.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val ctx get() = getApplication<Application>()

    val appointments: StateFlow<List<Appointment>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun get(id: Long): Appointment? = dao.get(id)

    fun save(a: Appointment, onDone: () -> Unit = {}) = viewModelScope.launch {
        val rowId = dao.upsert(a)
        val saved = if (a.id == 0L) a.copy(id = rowId) else a
        ReminderScheduler.schedule(ctx, saved)
        onDone()
    }

    fun delete(a: Appointment) = viewModelScope.launch {
        ReminderScheduler.cancel(ctx, a.id)
        NotificationHelper.cancel(ctx, a.id)
        dao.delete(a)
    }

    fun setStatus(a: Appointment, s: Status) = save(a.copy(status = s.name))
    fun togglePaid(a: Appointment) = save(a.copy(paid = !a.paid))
    fun setReminder(a: Appointment, on: Boolean) = save(a.copy(reminderEnabled = on))
    fun snooze(a: Appointment, minutes: Int) = ReminderScheduler.snooze(ctx, a.id, minutes)

    fun markCompleted(list: List<Appointment>) = viewModelScope.launch {
        list.forEach { ReminderScheduler.cancel(ctx, it.id) }
        dao.upsertAll(list.map { it.copy(status = Status.COMPLETED.name) })
    }
}
