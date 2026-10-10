package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DutyLog
import com.example.data.UserProfile
import com.example.util.TimeUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Auto Bill", appName)
  }

  @Test
  fun `verify Room user profile and duty log storage`() = runBlocking {
    val profile = UserProfile(
      id = 1,
      name = "Md. Fahim Ahmed Shimul",
      studentId = "2310866",
      accountNo = "1311001730864",
      bankName = "MTB"
    )
    db.userProfileDao().saveProfile(profile)
    val savedProfile = db.userProfileDao().getUserProfile().first()
    assertNotNull(savedProfile)
    assertEquals("Md. Fahim Ahmed Shimul", savedProfile?.name)
    assertEquals("2310866", savedProfile?.studentId)

    val logs = listOf(
      DutyLog(date = "2026-08-03", day = "Monday", startTime = "10:00", endTime = "13:00", duration = 3.0),
      DutyLog(date = "2026-08-03", day = "Monday", startTime = "14:30", endTime = "16:00", duration = 1.5)
    )
    db.dutyLogDao().insertLogs(logs)

    val allLogs = db.dutyLogDao().getAllLogsAscending().first()
    assertEquals(2, allLogs.size)

    val grouped = TimeUtil.groupLogsByDate(allLogs)
    assertEquals(1, grouped.size)
    assertEquals(4.5, grouped[0].totalHours, 0.001)
  }
}
