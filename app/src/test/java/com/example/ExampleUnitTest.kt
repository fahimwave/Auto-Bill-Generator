package com.example

import com.example.data.DutyLog
import com.example.util.RoutineUtil
import com.example.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAutoRoundingEngineStandardShifts() {
        val duration1 = TimeUtil.calculateRoundedDuration("10:00", "13:00")
        assertEquals(3.0, duration1, 0.001)

        val duration2 = TimeUtil.calculateRoundedDuration("14:30", "16:00")
        assertEquals(1.5, duration2, 0.001)

        val combined = duration1 + duration2
        assertEquals(4.5, combined, 0.001)
    }

    @Test
    fun testAutoRoundingStrictHalfHourIntervals() {
        val testPairs = listOf(
            Pair("10:00", "11:10"),
            Pair("10:00", "11:20"),
            Pair("10:00", "11:50"),
            Pair("10:00", "12:10"),
            Pair("10:00", "12:20")
        )

        for ((start, end) in testPairs) {
            val rounded = TimeUtil.calculateRoundedDuration(start, end)
            val remainder = (rounded * 10) % 5
            assertEquals("Duration $rounded must be in 0.5 hr intervals", 0.0, remainder, 0.001)
        }
    }

    @Test
    fun testMultiShiftGroupingAndAscendingSorting() {
        val logs = listOf(
            DutyLog(date = "2026-08-10", day = "Monday", startTime = "14:30", endTime = "16:00", duration = 1.5),
            DutyLog(date = "2026-08-03", day = "Monday", startTime = "10:00", endTime = "13:00", duration = 3.0),
            DutyLog(date = "2026-08-10", day = "Monday", startTime = "10:00", endTime = "13:00", duration = 3.0),
            DutyLog(date = "2026-08-03", day = "Monday", startTime = "14:30", endTime = "16:00", duration = 1.5)
        )

        val groups = TimeUtil.groupLogsByDate(logs)
        assertEquals(2, groups.size)
        assertEquals("2026-08-03", groups[0].dateIso)
        assertEquals("2026-08-10", groups[1].dateIso)

        assertEquals(2, groups[0].shifts.size)
        assertEquals(4.5, groups[0].totalHours, 0.001)

        assertEquals(2, groups[1].shifts.size)
        assertEquals(4.5, groups[1].totalHours, 0.001)

        val grandTotal = groups.sumOf { it.totalHours }
        assertEquals(9.0, grandTotal, 0.001)
    }

    @Test
    fun testRoutineConsecutiveSlotMerging() {
        // 10:00-11:30 and 11:30-13:00 should merge into 10:00-13:00
        val slots = listOf(
            Pair("10:00", "11:30"),
            Pair("11:30", "13:00"),
            Pair("14:30", "16:00")
        )
        val merged = RoutineUtil.mergeConsecutiveSlots(slots)
        assertEquals(2, merged.size)
        assertEquals(Pair("10:00", "13:00"), merged[0])
        assertEquals(Pair("14:30", "16:00"), merged[1])
    }

    @Test
    fun testRoutineGenerateMonthLogs() {
        // Generate for August 2026 with ST and MW slots
        val logs = RoutineUtil.generateMonthDutyLogs(
            year = 2026,
            month = 8,
            selectedStSlotIds = setOf("ST_1", "ST_2", "ST_4"), // 10:00-13:00 and 14:30-16:00 (4.5h)
            selectedMwSlotIds = setOf("MW_1", "MW_2"),         // 10:00-13:00 (3.0h)
            selectedThSlotIds = setOf("TH_1")                  // 11:00-13:00 (2.0h)
        )

        assertTrue("Generated logs should not be empty", logs.isNotEmpty())
        // Verify no Fridays or Saturdays are in the duty logs
        for (log in logs) {
            assertTrue("Friday must not have duty", log.day != "Friday")
            assertTrue("Saturday must not have duty", log.day != "Saturday")
        }
    }
}
