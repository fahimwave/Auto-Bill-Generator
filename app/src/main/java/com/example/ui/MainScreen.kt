package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.AscDatePickerDialog
import com.example.ui.components.EditShiftDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.WheelTimePickerDialog
import com.example.ui.theme.IubNavyPrimary
import com.example.ui.theme.IubOrangeAccent
import com.example.util.DayDutyGroup
import com.example.util.PdfGenerator
import com.example.util.RoutineUtil
import com.example.util.TimeSlotDef
import com.example.util.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormatSymbols
import java.util.Locale

data class ShiftDeleteTarget(
    val id: Int,
    val timeLabel: String,
    val dateLabel: String
)

data class ShiftEditTarget(
    val id: Int,
    val startTime: String,
    val endTime: String,
    val dateLabel: String,
    val dayName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DutyViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDateIso by viewModel.selectedDateIso.collectAsState()
    val shiftDrafts by viewModel.shiftDrafts.collectAsState()
    val groupedDays by viewModel.groupedDays.collectAsState()
    val grandTotalHours by viewModel.grandTotalHours.collectAsState()
    val currentDraftTotalHours by viewModel.currentDraftTotalHours.collectAsState()

    // Routine Roster State
    val selectedStSlots by viewModel.selectedStSlots.collectAsState()
    val selectedMwSlots by viewModel.selectedMwSlots.collectAsState()
    val selectedThSlots by viewModel.selectedThSlots.collectAsState()
    val routineMonth by viewModel.routineMonth.collectAsState()
    val routineYear by viewModel.routineYear.collectAsState()

    // Smooth finger swipeable pager with 2 pages: 0 = Routine & Entry, 1 = Bill Preview
    val pagerState = rememberPagerState(pageCount = { 2 })

    var showProfileDialog by remember { mutableStateOf(false) }
    var pdfGeneratedFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }

    // Dialog state for editing a shift row (Late entry)
    var shiftToEdit by remember { mutableStateOf<ShiftEditTarget?>(null) }

    // State for Time Picker Dialog in manual entry
    var activeTimePickerShiftIndex by remember { mutableStateOf<Int?>(null) }
    var isEditingStartTime by remember { mutableStateOf(true) }

    // State for Date Picker Dialog with highlighted selected date
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Listen to UI event toasts - show briefly (1.2s) so user is never annoyed
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(1200L)
            job.cancel()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Profile Menu",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(modifier = Modifier.fillMaxWidth().padding(end = 12.dp)) {
                        Text(
                            text = userProfile.name.ifBlank { "Tap menu to set Name" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (userProfile.studentId.isNotBlank()) "ID: ${userProfile.studentId}" else "Student ID not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IubNavyPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode Toggle Tab Bar
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = IubOrangeAccent
                    )
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (pagerState.currentPage == 0) IubOrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Weekly Routine",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (pagerState.currentPage == 1) IubOrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Bill Preview",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // Finger-Swipeable Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        RoutineAndEntryScreen(
                            selectedStSlots = selectedStSlots,
                            selectedMwSlots = selectedMwSlots,
                            selectedThSlots = selectedThSlots,
                            routineMonth = routineMonth,
                            routineYear = routineYear,
                            onToggleSt = { id -> viewModel.toggleStSlot(id) },
                            onToggleMw = { id -> viewModel.toggleMwSlot(id) },
                            onToggleTh = { id -> viewModel.toggleThSlot(id) },
                            onSetMonth = { m -> viewModel.setRoutineMonth(m) },
                            onSetYear = { y -> viewModel.setRoutineYear(y) },
                            onGenerateMonth = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.generateMonthlyDutyRoster { _ ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = 1,
                                            animationSpec = tween(
                                                durationMillis = 650,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            // Manual Entry Props
                            selectedDateIso = selectedDateIso,
                            shiftDrafts = shiftDrafts,
                            currentDraftTotalHours = currentDraftTotalHours,
                            onOpenDatePicker = { showDatePickerDialog = true },
                            onAddAnotherShift = { viewModel.addAnotherShift() },
                            onRemoveShift = { idx -> viewModel.removeShift(idx) },
                            onOpenTimePicker = { idx, isStart ->
                                activeTimePickerShiftIndex = idx
                                isEditingStartTime = isStart
                            },
                            onSaveManualEntry = {
                                viewModel.saveCurrentEntry {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Extra Duty Saved!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    1 -> {
                        LiveBillPreviewScreen(
                            userProfile = userProfile,
                            groupedDays = groupedDays,
                            grandTotalHours = grandTotalHours,
                            routineYear = routineYear,
                            routineMonth = routineMonth,
                            onGeneratePdf = {
                                val file = viewModel.generatePdf()
                                if (file != null) {
                                    pdfGeneratedFile = file
                                    showPdfSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEditShift = { target -> shiftToEdit = target },
                            onDeleteShift = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.deleteLog(id)
                            },
                            onClearAll = { viewModel.clearAllLogs() }
                        )
                    }
                }
            }
        }
    }

    // Edit Shift Dialog for Late Arrival / Adjusted Times
    shiftToEdit?.let { target ->
        EditShiftDialog(
            shiftId = target.id,
            initialStartTime = target.startTime,
            initialEndTime = target.endTime,
            dateDisplay = target.dateLabel,
            dayName = target.dayName,
            onSave = { id, newStart, newEnd ->
                viewModel.updateShiftTime(id, newStart, newEnd)
                shiftToEdit = null
            },
            onDismiss = { shiftToEdit = null }
        )
    }

    // Modern Tactile Time Picker Dialog (10:00 - 16:00, including 11:00 & 11:30)
    activeTimePickerShiftIndex?.let { index ->
        val shift = shiftDrafts.getOrNull(index)
        if (shift != null) {
            val isStart = isEditingStartTime
            WheelTimePickerDialog(
                title = if (isStart) "Select 'From' Time (Shift ${index + 1})" else "Select 'To' Time (Shift ${index + 1})",
                initialTime = if (isStart) shift.startTime else shift.endTime,
                onTimeSelected = { newTime ->
                    if (isStart) {
                        viewModel.updateShiftStartTime(index, newTime)
                    } else {
                        viewModel.updateShiftEndTime(index, newTime)
                    }
                    activeTimePickerShiftIndex = null
                },
                onDismiss = { activeTimePickerShiftIndex = null }
            )
        }
    }

    // Highlighted Date Picker Dialog
    if (showDatePickerDialog) {
        AscDatePickerDialog(
            initialDateIso = selectedDateIso,
            onDateSelected = { pickedIso ->
                viewModel.setDate(pickedIso)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // Profile Dialog opened via hamburger menu
    if (showProfileDialog) {
        ProfileDialog(
            currentProfile = userProfile,
            onSaveProfile = { updated ->
                viewModel.saveUserProfile(updated)
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    // PDF Downloaded Dialog
    if (showPdfSuccessDialog && pdfGeneratedFile != null) {
        val file = pdfGeneratedFile!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = IubOrangeAccent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "PDF Downloaded",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your duty bill PDF has been generated and is ready to view or share.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: ${file.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        PdfGenerator.openPdf(context, file)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IubOrangeAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open / View PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        PdfGenerator.sharePdf(context, file)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

/**
 * Screen A: Weekly Routine Setup & Auto-Generator (The core automated feature)
 * Plus optional expander for manual single-shift entry.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineAndEntryScreen(
    selectedStSlots: Set<String>,
    selectedMwSlots: Set<String>,
    selectedThSlots: Set<String>,
    routineMonth: Int,
    routineYear: Int,
    onToggleSt: (String) -> Unit,
    onToggleMw: (String) -> Unit,
    onToggleTh: (String) -> Unit,
    onSetMonth: (Int) -> Unit,
    onSetYear: (Int) -> Unit,
    onGenerateMonth: () -> Unit,
    // Manual entry props
    selectedDateIso: String,
    shiftDrafts: List<ShiftDraft>,
    currentDraftTotalHours: Double,
    onOpenDatePicker: () -> Unit,
    onAddAnotherShift: () -> Unit,
    onRemoveShift: (Int) -> Unit,
    onOpenTimePicker: (Int, Boolean) -> Unit,
    onSaveManualEntry: () -> Unit
) {
    var showManualSection by remember { mutableStateOf(false) }

    val months = remember {
        val symbols = DateFormatSymbols(Locale.US)
        symbols.months.take(12)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Core Feature Card: Weekly Routine Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = IubOrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Weekly Routine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Year Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IubOrangeAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$routineYear",
                                fontWeight = FontWeight.Bold,
                                color = IubOrangeAccent,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Select your routine slots once to auto-generate the month's bill.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // Month Selector Chips
                    Text(
                        text = "Target Billing Month:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val monthListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = (routineMonth - 1).coerceAtLeast(0)
                    )

                    LaunchedEffect(routineMonth) {
                        monthListState.animateScrollToItem((routineMonth - 1).coerceAtLeast(0))
                    }

                    LazyRow(
                        state = monthListState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(months.indices.toList()) { idx ->
                            val mNum = idx + 1
                            val isSelected = mNum == routineMonth
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetMonth(mNum) },
                                label = {
                                    Text(
                                        text = months[idx],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IubOrangeAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // 1. ST Time Slots (Sunday & Tuesday)
                    RosterSection(
                        title = "ST (Sunday & Tuesday)",
                        slots = RoutineUtil.ST_SLOTS,
                        selectedSlotIds = selectedStSlots,
                        onToggle = onToggleSt
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. MW Time Slots (Monday & Wednesday)
                    RosterSection(
                        title = "MW (Monday & Wednesday)",
                        slots = RoutineUtil.MW_SLOTS,
                        selectedSlotIds = selectedMwSlots,
                        onToggle = onToggleMw
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Thursday
                    RosterSection(
                        title = "Thursday",
                        slots = RoutineUtil.TH_SLOTS,
                        selectedSlotIds = selectedThSlots,
                        onToggle = onToggleTh
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Generate Button
                    Button(
                        onClick = onGenerateMonth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IubOrangeAccent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-Generate Full Month Duties",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Optional Section: Log Single Missed/Extra Duty Outside Routine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualSection = !showManualSection },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MoreTime, contentDescription = null, tint = IubOrangeAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Extra Duty Hour",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (showManualSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showManualSection) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            // Date row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Date: $selectedDateIso",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = onOpenDatePicker,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp), tint = IubOrangeAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            shiftDrafts.forEachIndexed { index, shift ->
                                ShiftInputRow(
                                    shiftNumber = index + 2,
                                    canDelete = shiftDrafts.size > 1,
                                    startTime = shift.startTime,
                                    endTime = shift.endTime,
                                    duration = shift.duration,
                                    onFromClick = { onOpenTimePicker(index, true) },
                                    onToClick = { onOpenTimePicker(index, false) },
                                    onDelete = { onRemoveShift(index) }
                                )
                                if (index < shiftDrafts.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onAddAnotherShift,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = IubOrangeAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Shift", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = onSaveManualEntry,
                                    modifier = Modifier.weight(1.2f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IubOrangeAccent, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Extra Duty", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RosterSection(
    title: String,
    slots: List<TimeSlotDef>,
    selectedSlotIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slots.forEach { slot ->
                val isSelected = slot.id in selectedSlotIds
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) IubOrangeAccent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onToggle(slot.id) }
                        .border(
                            1.dp,
                            if (isSelected) IubOrangeAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftInputRow(
    shiftNumber: Int,
    canDelete: Boolean,
    startTime: String,
    endTime: String,
    duration: Double,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Shift badge & duration
            Column(
                modifier = Modifier.width(62.dp)
            ) {
                Text(
                    text = "Shift $shiftNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IubOrangeAccent,
                    maxLines = 1
                )
                Text(
                    text = "${TimeUtil.formatDurationString(duration)} hr",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Time Selector: Column layout inside each card (label top, time value bottom)
            // This guarantees '10:00' never wraps even on narrow screens
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // From Time Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onFromClick() }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "From",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = startTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "→",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))

                // To Time Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToClick() }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "To",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = endTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Delete action button or spacing
            if (canDelete) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Remove Shift",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/**
 * Screen B: Bill Preview View (Interactive table with click-to-edit for late arrivals,
 * and cross-delete for missed/absent/holiday days)
 */
@Composable
fun LiveBillPreviewScreen(
    userProfile: com.example.data.UserProfile,
    groupedDays: List<DayDutyGroup>,
    grandTotalHours: Double,
    routineYear: Int,
    routineMonth: Int,
    onGeneratePdf: () -> Unit,
    onEditShift: (ShiftEditTarget) -> Unit,
    onDeleteShift: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var shiftTargetToDelete by remember { mutableStateOf<ShiftDeleteTarget?>(null) }
    var isProfileExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official ASC Document Preview Header with Minimizable Student Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Independent University, Bangladesh",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Academic Success Center (ASC) • Duty Bill",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TimeUtil.getBillingMonthTitle(routineYear, routineMonth),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = IubOrangeAccent
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    // Minimizable Student Details Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProfileExpanded = !isProfileExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.name.ifBlank { "Name: [Not Set - Tap to edit]" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isProfileExpanded) {
                                val idText = if (userProfile.studentId.isNotBlank()) "ID: ${userProfile.studentId}" else "ID: [Not Set]"
                                val bankText = "Bank: ${userProfile.bankName.ifBlank { "MTB" }}"
                                Text(
                                    text = "$idText • $bankText",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isProfileExpanded) "Minimize" else "Details",
                                fontSize = 11.5.sp,
                                color = IubOrangeAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isProfileExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isProfileExpanded) "Minimize details" else "Expand details",
                                tint = IubOrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Expanded Details View
                    AnimatedVisibility(visible = isProfileExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Name: ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(userProfile.name.ifBlank { "Not configured yet" }, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("ID: ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(userProfile.studentId.ifBlank { "Not configured yet" }, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Account no: ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(userProfile.accountNo.ifBlank { "Not configured yet" }, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Bank Name: ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(userProfile.bankName.ifBlank { "MTB" }, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Official ASC Table Replicating LaTeX Specification with per-shift edit on click & cross delete
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ASC Duty Hours Table",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = IubOrangeAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap any shift to edit arrival time (if late)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        if (groupedDays.isNotEmpty()) {
                            TextButton(onClick = { showClearConfirmation = true }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Table with Row-Click Editing & Cross Deletion
                    OfficialAscTableView(
                        groupedDays = groupedDays,
                        grandTotalHours = grandTotalHours,
                        onEditShift = onEditShift,
                        onRequestDeleteShift = { target ->
                            shiftTargetToDelete = target
                        }
                    )
                }
            }
        }

        // "Download PDF" Button
        item {
            Button(
                onClick = onGeneratePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IubOrangeAccent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Download PDF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Confirmation Warning Dialog for deleting a specific shift (Absent / Holiday / Missed) - Fully Centered
    shiftTargetToDelete?.let { target ->
        Dialog(onDismissRequest = { shiftTargetToDelete = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Remove Duty Shift?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Remove shift ${target.timeLabel} on ${target.dateLabel}?\nTotal hours will recalculate automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shiftTargetToDelete = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onDeleteShift(target.id)
                                shiftTargetToDelete = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "Remove",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clear All
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Duty Logs?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will remove all duty entries from the database. This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
fun OfficialAscTableView(
    groupedDays: List<DayDutyGroup>,
    grandTotalHours: Double,
    onEditShift: (ShiftEditTarget) -> Unit,
    onRequestDeleteShift: (ShiftDeleteTarget) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "Date", weight = 1.5f, isHeader = true)
            TableCell(text = "Day", weight = 1.7f, isHeader = true)
            TableCell(text = "From", weight = 1.2f, isHeader = true)
            TableCell(text = "To", weight = 1.2f, isHeader = true)
            TableCell(text = "Hours", weight = 1.1f, isHeader = true)
            TableCell(text = "", weight = 0.5f, isHeader = true) // Column for Delete cross icon
        }

        HorizontalDivider(color = borderColor)

        AnimatedContent(
            targetState = groupedDays,
            transitionSpec = {
                (fadeIn(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
                 slideInVertically(animationSpec = tween(450, easing = FastOutSlowInEasing)) { 16 })
                    .togetherWith(
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                    )
            },
            label = "TableContentTransition"
        ) { targetDays ->
            if (targetDays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No duty logs generated yet. Set your weekly routine and tap 'Auto-Generate'!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    targetDays.forEachIndexed { _, dayGroup ->
                        // Multi-shift day row representation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Date Column (weight 1.5f, spans all shifts of the day)
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .padding(horizontal = 2.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayGroup.dateDisplay,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 2. Day Column (weight 1.7f, single line without wrapping 'y' below)
                            Box(
                                modifier = Modifier
                                    .weight(1.7f)
                                    .padding(horizontal = 2.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayGroup.dayName,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 3 & 4. From & To Columns (Combined weight 2.4f = 1.2f + 1.2f)
                            Column(modifier = Modifier.weight(2.4f)) {
                                dayGroup.shifts.forEachIndexed { sIdx, shift ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEditShift(
                                                    ShiftEditTarget(
                                                        id = shift.id,
                                                        startTime = shift.startTime,
                                                        endTime = shift.endTime,
                                                        dateLabel = dayGroup.dateDisplay,
                                                        dayName = dayGroup.dayName
                                                    )
                                                )
                                            }
                                            .padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = shift.startTime,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1.2f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = shift.endTime,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1.2f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (sIdx < dayGroup.shifts.size - 1) {
                                        HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                                    }
                                }
                            }

                            // 5. Total Hours for Day (weight 1.1f, perfectly aligned with Hours column)
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .padding(horizontal = 2.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = TimeUtil.formatDurationString(dayGroup.totalHours),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IubOrangeAccent,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 6. Cross Delete Action Column (weight 0.5f, aligned with header action column)
                            Column(modifier = Modifier.weight(0.5f)) {
                                dayGroup.shifts.forEachIndexed { sIdx, shift ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onRequestDeleteShift(
                                                    ShiftDeleteTarget(
                                                        id = shift.id,
                                                        timeLabel = "${shift.startTime} - ${shift.endTime}",
                                                        dateLabel = "${dayGroup.dayName}, ${dayGroup.dateDisplay}"
                                                    )
                                                )
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove shift",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    if (sIdx < dayGroup.shifts.size - 1) {
                                        HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor)
                    }
                }
            }
        }

        // Table Footer: \multicolumn{4}{|r|}{\textbf{Total Hours:}} & \textbf{9.0} \\ \hline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Hours:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            AnimatedContent(
                targetState = grandTotalHours,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(150))
                },
                label = "TotalHoursTransition"
            ) { total ->
                Text(
                    text = "${TimeUtil.formatDurationString(total)} hrs",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = IubOrangeAccent
                )
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (isHeader) 12.5.sp else 12.sp,
        color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp)
    )
}
