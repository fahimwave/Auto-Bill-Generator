package com.example.util

import com.example.data.DutyLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimeSlotDef(
    val id: String,
    val startTime: String,
    val endTime: String,
    val label: String
)

object RoutineUtil {

    // Standard IUB ASC Roster Slots from Official Schedule
    val ST_SLOTS = listOf(
        TimeSlotDef("ST_1", "10:00", "11:30", "10:00 - 11:30"),
        TimeSlotDef("ST_2", "11:30", "13:00", "11:30 - 13:00"),
        TimeSlotDef("ST_3", "13:00", "14:30", "13:00 - 14:30"),
        TimeSlotDef("ST_4", "14:30", "16:00", "14:30 - 16:00")
    )

    val MW_SLOTS = listOf(
        TimeSlotDef("MW_1", "10:00", "11:30", "10:00 - 11:30"),
        TimeSlotDef("MW_2", "11:30", "13:00", "11:30 - 13:00"),
        TimeSlotDef("MW_3", "13:00", "14:30", "13:00 - 14:30"),
        TimeSlotDef("MW_4", "14:30", "16:00", "14:30 - 16:00")
    )

    val TH_SLOTS = listOf(
        TimeSlotDef("TH_1", "11:00", "13:00", "11:00 - 13:00"),
        TimeSlotDef("TH_2", "13:00", "15:00", "13:00 - 15:00")
    )

    /**
     * Merges adjacent consecutive slots (e.g. 10:00-11:30 and 11:30-13:00 -> 10:00-13:00)
     * to replicate the official ASC bill shift representation.
     */
    fun mergeConsecutiveSlots(slots: List<Pair<String, String>>): List<Pair<String, String>> {
        if (slots.size <= 1) return slots

        val sorted = slots.sortedBy { it.first }
        val merged = mutableListOf<Pair<String, String>>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (current.second == next.first) {
                // Consecutive: merge them into a single continuous shift
                current = Pair(current.first, next.second)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Auto-Generates the entire month's duty logs based on the user's weekly routine:
     * - Sunday & Tuesday: ST slots
     * - Monday & Wednesday: MW slots
     * - Thursday: TH slots
     * - Friday & Saturday: Off-duty (skipped)
     */
    fun generateMonthDutyLogs(
        year: Int,
        month: Int, // 1 to 12
        selectedStSlotIds: Set<String>,
        selectedMwSlotIds: Set<String>,
        selectedThSlotIds: Set<String>
    ): List<DutyLog> {
        val stPairs = ST_SLOTS.filter { it.id in selectedStSlotIds }.map { Pair(it.startTime, it.endTime) }
        val mwPairs = MW_SLOTS.filter { it.id in selectedMwSlotIds }.map { Pair(it.startTime, it.endTime) }
        val thPairs = TH_SLOTS.filter { it.id in selectedThSlotIds }.map { Pair(it.startTime, it.endTime) }

        val mergedSt = mergeConsecutiveSlots(stPairs)
        val mergedMw = mergeConsecutiveSlots(mwPairs)
        val mergedTh = mergeConsecutiveSlots(thPairs)

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayNameFormat = SimpleDateFormat("EEEE", Locale.US)
        val result = mutableListOf<DutyLog>()

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val activeSlots = when (dayOfWeek) {
                Calendar.SUNDAY, Calendar.TUESDAY -> mergedSt
                Calendar.MONDAY, Calendar.WEDNESDAY -> mergedMw
                Calendar.THURSDAY -> mergedTh
                else -> emptyList() // Friday and Saturday are off
            }

            if (activeSlots.isNotEmpty()) {
                val dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                val dayName = dayNameFormat.format(cal.time)

                for (slot in activeSlots) {
                    val duration = TimeUtil.calculateRoundedDuration(slot.first, slot.second)
                    result.add(
                        DutyLog(
                            date = dateIso,
                            day = dayName,
                            startTime = slot.first,
                            endTime = slot.second,
                            duration = duration
                        )
                    )
                }
            }
        }

        return result
    }
}
