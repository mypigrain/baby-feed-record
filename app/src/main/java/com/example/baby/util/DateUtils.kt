package com.example.baby.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINESE)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

    fun formatTime(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(timeFormatter)
    }

    fun formatDate(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(dateFormatter)
    }

    fun formatFullDate(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(fullDateFormatter)
    }

    fun formatDayOfWeek(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(dayOfWeekFormatter)
    }

    fun getDayStart(epochMillis: Long): Long {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getDayEnd(epochMillis: Long): Long {
        return getDayStart(epochMillis) + 86400000L
    }

    fun getWeekStart(): Long {
        val today = LocalDate.now()
        val dayOfWeek = today.get(WeekFields.of(Locale.getDefault()).dayOfWeek())
        val monday = today.minusDays(dayOfWeek.toLong() - 1)
        return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getWeekEnd(): Long {
        return getWeekStart() + 7 * 86400000L
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        return getDayStart(millis1) == getDayStart(millis2)
    }

    fun getDayLabel(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        val today = LocalDate.now()
        val date = ldt.toLocalDate()
        return when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            date == today.minusDays(2) -> "前天"
            else -> date.format(dateFormatter) + " " + date.format(dayOfWeekFormatter)
        }
    }
}
