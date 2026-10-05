package com.megs.consultation.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Status { UPCOMING, COMPLETED, CANCELLED }

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // Patient / Doctor / Clinic name
    val type: String = "Consultation",      // Consultation, Follow-up, Procedure...
    val dateTime: Long,                     // epoch millis
    val place: String,
    val fee: Double? = null,                // optional
    val notes: String = "",
    val phone: String = "",
    val status: String = Status.UPCOMING.name,
    val paid: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderOffsets: String = "60",     // CSV of minutes before, e.g. "1440,60"
    val soundAlert: Boolean = true,         // true = alarm sound + full screen, false = silent notification
    val createdAt: Long = System.currentTimeMillis()
)

fun Appointment.statusEnum(): Status =
    runCatching { Status.valueOf(status) }.getOrDefault(Status.UPCOMING)

fun Appointment.offsets(): List<Int> =
    reminderOffsets.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sortedDescending()

const val TEST_ID = -1L

fun demoAppointment() = Appointment(
    id = TEST_ID,
    name = "Test reminder",
    type = "Consultation",
    dateTime = System.currentTimeMillis(),
    place = "Clinic",
    fee = 500.0,
    notes = "This is how your appointment reminder will look."
)
