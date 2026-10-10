package com.example.util

import com.example.data.DutyLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ShiftItem(
    val id: Int = 0,
    val startTime: String,
    val endTime: String,
    val duration: Double
)

data class DayDutyGroup(
    val dateIso: String,
    val dateDisplay: String,     // e.g. "3-8-26"
    val dayName: String,         // e.g. "Monday"
    val shifts: List<ShiftItem>,
    val totalHours: Double
)

object TimeUtil {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("d-M-yy", Locale.US)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun getTodayIso(): String {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY -> cal.add(Calendar.DAY_OF_MONTH, -1)   // Thursday
            Calendar.SATURDAY -> cal.add(Calendar.DAY_OF_MONTH, -2) // Thursday
        }
        return isoFormat.format(cal.time)
    }

    fun getTodayDayName(): String {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
            Calendar.SATURDAY -> cal.add(Calendar.DAY_OF_MONTH, -2)
        }
        return dayOfWeekFormat.format(cal.time)
    }

    fun parseTime(timeStr: String): Pair<Int, Int> {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Pair(h, m)
    }

    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /**
     * Auto-Rounding Engine:
     * Calculates duration between start and end time and rounds strictly
     * to the nearest 0.5 hour interval (e.g. 0.5, 1.0, 1.5, 2.0, 4.5).
     * Fractional hours like 1.2 or 1.9 are strictly forbidden.
     */
    fun calculateRoundedDuration(startTime: String, endTime: String): Double {
        val (startH, startM) = parseTime(startTime)
        val (endH, endM) = parseTime(endTime)

        val startTotalMinutes = startH * 60 + startM
        val endTotalMinutes = endH * 60 + endM

        val diffMinutes = if (endTotalMinutes >= startTotalMinutes) {
            endTotalMinutes - startTotalMinutes
        } else {
            // Handles overnight shift gracefully
            (24 * 60 - startTotalMinutes) + endTotalMinutes
        }

        if (diffMinutes <= 0) return 0.0

        // Round to nearest 30-minute interval (0.5 hour)
        val halfUnits = (diffMinutes / 30.0).roundToInt()
        // If the user picked two distinct times but under 15 mins, guarantee at least 0.5 hr if non-zero
        val finalHalfUnits = if (halfUnits == 0 && diffMinutes > 0) 1 else halfUnits
        return finalHalfUnits * 0.5
    }

    fun formatDurationString(hours: Double): String {
        return String.format(Locale.US, "%.1f", hours)
    }

    fun getDayNameFromIso(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            dayOfWeekFormat.format(date)
        } catch (e: Exception) {
            "Monday"
        }
    }

    /**
     * Converts ISO date "2026-08-03" to official ASC LaTeX style "3-8-26"
     */
    fun formatToOfficialAscDate(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Returns "Bill of August 2026" based on year and month numbers (1-12)
     */
    fun getBillingMonthTitle(year: Int, month: Int): String {
        return try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                set(Calendar.DAY_OF_MONTH, 1)
            }
            "Bill of ${monthYearFormat.format(cal.time)}"
        } catch (e: Exception) {
            "Bill of Month $year"
        }
    }

    /**
     * Returns "Bill of August 2026" based on current date or list of logs
     */
    fun getBillingMonthTitle(logs: List<DutyLog>): String {
        return try {
            if (logs.isNotEmpty()) {
                val latest = logs.maxByOrNull { it.date }
                val date = latest?.let { isoFormat.parse(it.date) } ?: Date()
                "Bill of ${monthYearFormat.format(date)}"
            } else {
                "Bill of ${monthYearFormat.format(Date())}"
            }
        } catch (e: Exception) {
            "Bill of ${monthYearFormat.format(Date())}"
        }
    }

    /**
     * Groups multiple shifts per day in Ascending Order by Date,
     * calculating combined total hours per date, matching LaTeX \multirow layout.
     */
    fun groupLogsByDate(logs: List<DutyLog>): List<DayDutyGroup> {
        val sortedLogs = logs.sortedWith(compareBy({ it.date }, { it.startTime }))
        val groupedByDate = sortedLogs.groupBy { it.date }

        return groupedByDate.map { (dateIso, shiftLogs) ->
            val shiftItems = shiftLogs.map {
                ShiftItem(
                    id = it.id,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    duration = it.duration
                )
            }
            val totalDayHours = shiftItems.sumOf { it.duration }
            DayDutyGroup(
                dateIso = dateIso,
                dateDisplay = formatToOfficialAscDate(dateIso),
                dayName = shiftLogs.firstOrNull()?.day ?: getDayNameFromIso(dateIso),
                shifts = shiftItems,
                totalHours = totalDayHours
            )
        }
    }
}
