package com.megs.consultation.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    private val inr: NumberFormat =
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    fun amount(v: Double): String = inr.format(v)
    fun rupee(v: Double?): String = if (v == null) "" else "₹ ${inr.format(v)}"

    private fun f(p: String, ms: Long) = SimpleDateFormat(p, Locale.getDefault()).format(Date(ms))
    fun time(ms: Long) = f("h:mm a", ms)
    fun date(ms: Long) = f("d MMMM yyyy", ms)
    fun dateShort(ms: Long) = f("d MMM", ms)
    fun dayHeader(ms: Long) = f("EEE, d MMM yyyy", ms)
    fun monthYear(ms: Long) = f("MMMM yyyy", ms)
    fun dd(ms: Long) = f("dd", ms)
    fun mon(ms: Long) = f("MMM", ms).uppercase()
    fun dowShort(ms: Long) = f("EEE", ms)
    fun dow(ms: Long) = f("EEEE", ms)
    fun dateTime(ms: Long) = f("d MMM, h:mm a", ms)

    fun offsetLabel(m: Int): String = when {
        m == 0 -> "At time"
        m % 1440 == 0 -> "${m / 1440} day${if (m / 1440 > 1) "s" else ""} before"
        m % 60 == 0 -> "${m / 60} hour${if (m / 60 > 1) "s" else ""} before"
        else -> "$m min before"
    }

    fun inLabel(m: Int): String = if (m == 0) "Starting now" else "In " + offsetLabel(m).removeSuffix(" before")

    fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

object TimeUtil {
    fun cal(ms: Long): Calendar = Calendar.getInstance().apply { timeInMillis = ms }

    fun startOfDay(ms: Long): Long = cal(ms).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfMonth(ms: Long): Long = cal(startOfDay(ms)).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    fun addMonths(ms: Long, n: Int): Long = cal(ms).apply { add(Calendar.MONTH, n) }.timeInMillis
    fun addDays(ms: Long, n: Int): Long = cal(ms).apply { add(Calendar.DAY_OF_MONTH, n) }.timeInMillis

    /** Next full hour; if [presetDay] > 0 keep that calendar day. */
    fun defaultSlot(presetDay: Long): Long {
        val c = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (presetDay > 0) {
            val p = cal(presetDay)
            c.set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH))
        }
        return c.timeInMillis
    }
}
