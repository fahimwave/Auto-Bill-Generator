package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DutyLog
import com.example.data.DutyRepository
import com.example.data.UserProfile
import com.example.util.DayDutyGroup
import com.example.util.PdfGenerator
import com.example.util.RoutineUtil
import com.example.util.TimeUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

enum class AppViewMode {
    DATA_ENTRY,
    BILL_PREVIEW
}

data class ShiftDraft(
    val startTime: String = "10:00",
    val endTime: String = "13:00"
) {
    val duration: Double
        get() = TimeUtil.calculateRoundedDuration(startTime, endTime)
}

class DutyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DutyRepository

    val userProfile: StateFlow<UserProfile>
    val allLogs: StateFlow<List<DutyLog>>

    // Live Single-page view mode switcher
    private val _viewMode = MutableStateFlow(AppViewMode.DATA_ENTRY)
    val viewMode: StateFlow<AppViewMode> = _viewMode.asStateFlow()

    // Weekly Routine Roster State (ST, MW, TH/R) - empty by default so user picks their own
    private val _selectedStSlots = MutableStateFlow<Set<String>>(emptySet())
    val selectedStSlots: StateFlow<Set<String>> = _selectedStSlots.asStateFlow()

    private val _selectedMwSlots = MutableStateFlow<Set<String>>(emptySet())
    val selectedMwSlots: StateFlow<Set<String>> = _selectedMwSlots.asStateFlow()

    private val _selectedThSlots = MutableStateFlow<Set<String>>(emptySet())
    val selectedThSlots: StateFlow<Set<String>> = _selectedThSlots.asStateFlow()

    // Routine Month and Year (Defaults to current month/year or August 2026 if matching blueprint)
    private val currentCal = Calendar.getInstance()
    private val _routineYear = MutableStateFlow(currentCal.get(Calendar.YEAR))
    val routineYear: StateFlow<Int> = _routineYear.asStateFlow()

    private val _routineMonth = MutableStateFlow(currentCal.get(Calendar.MONTH) + 1) // 1-12
    val routineMonth: StateFlow<Int> = _routineMonth.asStateFlow()

    // Manual Data Entry Form State (For Extra Duties - Defaults to Shift 2)
    private val _selectedDateIso = MutableStateFlow(TimeUtil.getTodayIso())
    val selectedDateIso: StateFlow<String> = _selectedDateIso.asStateFlow()

    private val _shiftDrafts = MutableStateFlow(listOf(ShiftDraft("13:00", "16:00")))
    val shiftDrafts: StateFlow<List<ShiftDraft>> = _shiftDrafts.asStateFlow()

    // Persistent in-memory draft cache per date so shifts remain fixed for each date
    private val _dateShiftDrafts = mutableMapOf<String, List<ShiftDraft>>()
    private val _userModifiedDates = mutableSetOf<String>()

    // Transient UI events (Success toasts, PDF generated, errors)
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    private val _lastGeneratedPdf = MutableStateFlow<File?>(null)
    val lastGeneratedPdf: StateFlow<File?> = _lastGeneratedPdf.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DutyRepository(db.userProfileDao(), db.dutyLogDao())

        userProfile = repository.userProfile
            .map { profile ->
                val p = profile ?: UserProfile()
                if (p.bankName.isBlank()) p.copy(bankName = "MTB") else p
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                UserProfile(bankName = "MTB")
            )

        allLogs = repository.allLogs
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // Keep drafts in sync with saved extra duties when logs are loaded
        viewModelScope.launch {
            allLogs.collect { logs ->
                val currentDate = _selectedDateIso.value
                if (!_userModifiedDates.contains(currentDate)) {
                    val logsForDate = logs.filter { it.date == currentDate }.sortedBy { it.startTime }
                    if (logsForDate.size > 1) {
                        val savedExtraDrafts = logsForDate.drop(1).map { ShiftDraft(it.startTime, it.endTime) }
                        _dateShiftDrafts[currentDate] = savedExtraDrafts
                        _shiftDrafts.value = savedExtraDrafts
                    }
                }
            }
        }
    }

    // Dynamic grouped logs for ASC Official Table representation
    val groupedDays: StateFlow<List<DayDutyGroup>> = allLogs
        .combine(MutableStateFlow(Unit)) { logs, _ ->
            TimeUtil.groupLogsByDate(logs)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val grandTotalHours: StateFlow<Double> = groupedDays
        .combine(MutableStateFlow(Unit)) { groups, _ ->
            groups.sumOf { it.totalHours }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

    val currentDraftTotalHours: StateFlow<Double> = shiftDrafts
        .combine(MutableStateFlow(Unit)) { drafts, _ ->
            drafts.sumOf { it.duration }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            3.0
        )

    fun toggleStSlot(slotId: String) {
        val current = _selectedStSlots.value.toMutableSet()
        if (slotId in current) current.remove(slotId) else current.add(slotId)
        _selectedStSlots.value = current
    }

    fun toggleMwSlot(slotId: String) {
        val current = _selectedMwSlots.value.toMutableSet()
        if (slotId in current) current.remove(slotId) else current.add(slotId)
        _selectedMwSlots.value = current
    }

    fun toggleThSlot(slotId: String) {
        val current = _selectedThSlots.value.toMutableSet()
        if (slotId in current) current.remove(slotId) else current.add(slotId)
        _selectedThSlots.value = current
    }

    fun setRoutineMonth(month: Int) {
        _routineMonth.value = month
    }

    fun setRoutineYear(year: Int) {
        _routineYear.value = year
    }

    /**
     * Auto-generates the entire month's duty roster following the weekly routine!
     */
    fun generateMonthlyDutyRoster(onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            val y = _routineYear.value
            val m = _routineMonth.value
            val st = _selectedStSlots.value
            val mw = _selectedMwSlots.value
            val th = _selectedThSlots.value

            val generatedLogs = RoutineUtil.generateMonthDutyLogs(
                year = y,
                month = m,
                selectedStSlotIds = st,
                selectedMwSlotIds = mw,
                selectedThSlotIds = th
            )

            if (generatedLogs.isEmpty()) {
                _uiEvents.emit("Please select at least one shift slot for ST, MW, or Thursday.")
                return@launch
            }

            // Clear existing duties and insert this month's generated roster so table never duplicates on multiple clicks
            repository.clearAll()
            repository.insertLogs(generatedLogs)

            _dateShiftDrafts.clear()
            _userModifiedDates.clear()
            loadDraftsForDate(_selectedDateIso.value)

            onSuccess(generatedLogs.size)
        }
    }

    /**
     * Updates an existing shift (e.g. if the student arrived late or departed early).
     */
    fun updateShiftTime(id: Int, newStartTime: String, newEndTime: String) {
        viewModelScope.launch {
            val roundedDuration = TimeUtil.calculateRoundedDuration(newStartTime, newEndTime)
            repository.updateLog(id, newStartTime, newEndTime, roundedDuration)
            val updatedLog = allLogs.value.find { it.id == id }
            if (updatedLog != null) {
                _dateShiftDrafts.remove(updatedLog.date)
                _userModifiedDates.remove(updatedLog.date)
                if (_selectedDateIso.value == updatedLog.date) {
                    loadDraftsForDate(updatedLog.date)
                }
            }
            _uiEvents.emit("Shift updated: $newStartTime - $newEndTime ($roundedDuration hrs)")
        }
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == AppViewMode.DATA_ENTRY) {
            AppViewMode.BILL_PREVIEW
        } else {
            AppViewMode.DATA_ENTRY
        }
    }

    fun setViewMode(mode: AppViewMode) {
        _viewMode.value = mode
    }

    fun setDate(dateIso: String) {
        _selectedDateIso.value = dateIso
        loadDraftsForDate(dateIso)
    }

    private fun loadDraftsForDate(dateIso: String) {
        // If user made unsaved manual edits for this date, keep them
        if (_userModifiedDates.contains(dateIso) && _dateShiftDrafts.containsKey(dateIso)) {
            _shiftDrafts.value = _dateShiftDrafts[dateIso] ?: listOf(ShiftDraft("13:00", "16:00"))
            return
        }

        // Check if database already has saved extra shifts for this date (shifts beyond index 0)
        val logsForDate = allLogs.value.filter { it.date == dateIso }.sortedBy { it.startTime }
        if (logsForDate.size > 1) {
            val savedExtraDrafts = logsForDate.drop(1).map { ShiftDraft(it.startTime, it.endTime) }
            _dateShiftDrafts[dateIso] = savedExtraDrafts
            _shiftDrafts.value = savedExtraDrafts
        } else if (_dateShiftDrafts.containsKey(dateIso)) {
            _shiftDrafts.value = _dateShiftDrafts[dateIso] ?: listOf(ShiftDraft("13:00", "16:00"))
        } else {
            val defaultDraft = listOf(ShiftDraft("13:00", "16:00"))
            _dateShiftDrafts[dateIso] = defaultDraft
            _shiftDrafts.value = defaultDraft
        }
    }

    fun addAnotherShift() {
        val current = _shiftDrafts.value
        val nextStart = if (current.isNotEmpty()) {
            current.last().endTime
        } else "16:00"
        val nextEnd = when (nextStart) {
            "13:00" -> "14:30"
            "14:30" -> "16:00"
            "15:00" -> "16:30"
            "16:00" -> "17:30"
            "17:00" -> "18:30"
            "17:30" -> "19:00"
            else -> "18:00"
        }
        val updated = current + ShiftDraft(nextStart, nextEnd)
        _shiftDrafts.value = updated
        val date = _selectedDateIso.value
        _dateShiftDrafts[date] = updated
        _userModifiedDates.add(date)
    }

    fun removeShift(index: Int) {
        val current = _shiftDrafts.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _shiftDrafts.value = current
            val date = _selectedDateIso.value
            _dateShiftDrafts[date] = current
            _userModifiedDates.add(date)
        }
    }

    fun updateShiftStartTime(index: Int, newStartTime: String) {
        val current = _shiftDrafts.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(startTime = newStartTime)
            _shiftDrafts.value = current
            val date = _selectedDateIso.value
            _dateShiftDrafts[date] = current
            _userModifiedDates.add(date)
        }
    }

    fun updateShiftEndTime(index: Int, newEndTime: String) {
        val current = _shiftDrafts.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(endTime = newEndTime)
            _shiftDrafts.value = current
            val date = _selectedDateIso.value
            _dateShiftDrafts[date] = current
            _userModifiedDates.add(date)
        }
    }

    fun saveCurrentEntry(onSuccessFeedback: () -> Unit) {
        viewModelScope.launch {
            val date = _selectedDateIso.value
            val dayName = TimeUtil.getDayNameFromIso(date)
            val drafts = _shiftDrafts.value

            val existingLogsForDate = allLogs.value.filter { it.date == date }.sortedBy { it.startTime }

            // If this date already has extra shifts (index 1 and beyond), remove them first
            // to allow clean updates without duplicate or stale shifts.
            // The primary routine shift (Shift 1 at index 0) is preserved completely!
            if (existingLogsForDate.size > 1) {
                val oldExtraLogs = existingLogsForDate.drop(1)
                oldExtraLogs.forEach { log ->
                    repository.deleteLog(log.id)
                }
            }

            val logsToInsert = drafts.map { draft ->
                DutyLog(
                    date = date,
                    day = dayName,
                    startTime = draft.startTime,
                    endTime = draft.endTime,
                    duration = draft.duration
                )
            }

            repository.insertLogs(logsToInsert)

            // Cache the saved extra shifts for this date so they remain permanently fixed
            // and do NOT revert to default values when navigating away or adding more shifts later!
            _dateShiftDrafts[date] = drafts
            _userModifiedDates.remove(date)
            _shiftDrafts.value = drafts

            onSuccessFeedback()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            val deletedLog = allLogs.value.find { it.id == id }
            repository.deleteLog(id)
            if (deletedLog != null) {
                _dateShiftDrafts.remove(deletedLog.date)
                _userModifiedDates.remove(deletedLog.date)
                if (_selectedDateIso.value == deletedLog.date) {
                    loadDraftsForDate(deletedLog.date)
                }
            }
        }
    }

    fun deleteDayLogs(dateIso: String) {
        viewModelScope.launch {
            repository.deleteLogsForDate(dateIso)
            _dateShiftDrafts.remove(dateIso)
            _userModifiedDates.remove(dateIso)
            if (_selectedDateIso.value == dateIso) {
                val defaultDraft = listOf(ShiftDraft("13:00", "16:00"))
                _shiftDrafts.value = defaultDraft
                _dateShiftDrafts[dateIso] = defaultDraft
            }
            _uiEvents.emit("Entries for $dateIso removed")
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAll()
            _dateShiftDrafts.clear()
            _userModifiedDates.clear()
            _shiftDrafts.value = listOf(ShiftDraft("13:00", "16:00"))
            _uiEvents.emit("All duty logs cleared")
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            val updated = if (profile.bankName.isBlank()) profile.copy(bankName = "MTB") else profile
            repository.saveProfile(updated)
            _uiEvents.emit("Profile updated successfully")
        }
    }

    fun generatePdf(): File? {
        val context = getApplication<Application>()
        return try {
            val file = PdfGenerator.generateAscBillPdf(
                context = context,
                profile = userProfile.value,
                logs = allLogs.value,
                billingTitleOverride = TimeUtil.getBillingMonthTitle(_routineYear.value, _routineMonth.value)
            )
            _lastGeneratedPdf.value = file
            file
        } catch (e: Exception) {
            null
        }
    }
}
