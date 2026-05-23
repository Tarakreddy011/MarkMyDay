package com.project.markmyday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.project.markmyday.ui.components.*
import com.project.markmyday.ui.models.DashboardTile
import com.project.markmyday.ui.models.TimetableEntry
import androidx.compose.ui.tooling.preview.Preview
import com.project.markmyday.ui.theme.MarkMyDayTheme
import com.project.markmyday.R
import com.project.markmyday.viewmodel.TimetableViewModel
import com.project.markmyday.viewmodel.DigitalDiaryViewModel


import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.viewmodel.NotificationViewModel
import com.project.markmyday.ui.navigation.Screen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userName: String = "Rahul Kumar",
    userRole: String = "Class 1 - A",
    studentId: String = "",
    uid: String = "",
    onNotificationClick: () -> Unit,
    onTileClick: (String) -> Unit,
    onNavigate: (String) -> Unit,
    navController: NavHostController
) {
    val notificationViewModel: NotificationViewModel = viewModel()
    val hasUnread by notificationViewModel.hasUnreadNotices.collectAsState()

    LaunchedEffect(userRole) {
        notificationViewModel.fetchNotifications(userRole)
    }

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                icon = Icons.Default.Face,
                title = stringResource(R.string.my_school_life),
                onNotificationClick = onNotificationClick,
                notificationCount = if (hasUnread) 1 else 0,
                onProfileClick = { onTileClick("settings") }
            )
        },
        bottomBar = {
            DashboardBottomBar(currentRoute = "dashboard", onNavigate = onNavigate)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            Spacer(modifier = Modifier.height(8.dp))
            StudentDashboardHomeContent(
                userName = userName,
                userRole = userRole,
                studentId = studentId,
                uid = uid,
                searchQuery = searchQuery,
                onTileClick = { id ->
                    when (id) {
                        "attendance" -> {
                            navController.navigate(Screen.StudentAttendanceDashboard.createRoute(uid))
                        }
                        "leave" -> {
                            navController.navigate(Screen.StudentLeave.route)
                        }
                        else -> onTileClick(id)
                    }
                }
            )
        }
    }
}

@Composable
fun StudentDashboardHomeContent(
    userName: String,
    userRole: String,
    studentId: String = "",
    uid: String = "",
    searchQuery: String = "",
    onTileClick: (String) -> Unit,
    viewModel: TimetableViewModel = viewModel(),
    diaryViewModel: DigitalDiaryViewModel = viewModel(),
    attendanceViewModel: com.project.markmyday.viewmodel.StudentAttendanceViewModel = viewModel(),
    quizViewModel: com.project.markmyday.viewmodel.QuizTakingViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timetables by viewModel.allTimetables.collectAsState()
    val homeworkBySubject by diaryViewModel.latestHomeworkBySubject.collectAsState()
    val attendanceSummary by attendanceViewModel.summary.collectAsState()
    val availableQuizzes by quizViewModel.availableQuizzes.collectAsState()
    
    // Decode userRole first, then parse class name from role (e.g., "Class 10 - A" -> "Class 10")
    val className = remember(userRole) {
        val decoded = try { java.net.URLDecoder.decode(userRole, "UTF-8") } catch (e: Exception) { userRole }
        val parts = decoded.split("-")
        if (parts.isNotEmpty()) parts[0].trim() else ""
    }

    LaunchedEffect(className, uid) {
        if (className.isNotEmpty()) {
            diaryViewModel.fetchEntriesForClass(className)
            quizViewModel.loadAvailableQuizzes(className)
        }
        if (uid.isNotEmpty()) {
            attendanceViewModel.fetchStudentAttendance(uid)
        }
    }

    val currentTimetable = timetables.find { it.className.equals(className, ignoreCase = true) }
    
    // Get today's classes
    val today = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Monday"
            java.util.Calendar.TUESDAY -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY -> "Thursday"
            java.util.Calendar.FRIDAY -> "Friday"
            java.util.Calendar.SATURDAY -> "Saturday"
            else -> ""
        }
    }

    val todayEntries = remember(currentTimetable, today) {
        val daySchedule = currentTimetable?.weeklySchedule?.get(today)
        daySchedule?.periods?.filter { it.subject.isNotEmpty() }?.map { period ->
            TimetableEntry(
                subject = period.subject,
                code = "P${period.periodNumber}",
                time = "${period.startTime} - ${period.endTime}",
                room = period.teacherName
            )
        } ?: emptyList()
    }

    val studentTiles = listOf(
        DashboardTile(
            "attendance", 
            stringResource(R.string.tile_attendance), 
            Icons.Default.CalendarMonth, 
            badgeText = if (attendanceSummary.totalWorkingDays > 0) "${attendanceSummary.overallAttendance.toInt()}%" else "N/A"
        ),
        DashboardTile("assignments", stringResource(R.string.tile_assignments), Icons.Default.Task, badgeCount = homeworkBySubject.size),
        DashboardTile("results", stringResource(R.string.tile_results), Icons.Default.Grade),
        DashboardTile("leave", stringResource(R.string.tile_leave), Icons.Default.HistoryEdu, badgeCount = if (attendanceSummary.leavesTaken > 0) attendanceSummary.leavesTaken else 0),
        DashboardTile("updates", stringResource(R.string.tile_updates), Icons.Default.Update),
        DashboardTile("exams", stringResource(R.string.tile_exams), Icons.Default.Quiz, badgeCount = if (availableQuizzes.isNotEmpty()) availableQuizzes.size else 0),
        DashboardTile("leaderboard", stringResource(R.string.leaderboard_title), Icons.Default.Leaderboard),
        DashboardTile("fees", stringResource(R.string.tile_fees), Icons.Default.Payments),
        DashboardTile("settings", stringResource(R.string.settings), Icons.Default.Settings)
    ).filter { it.label.contains(searchQuery, ignoreCase = true) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Welcome Header
        item(span = { GridItemSpan(2) }) {
            WelcomeSection(
                name = userName,
                role = userRole,
                date = getCurrentDate(),
                icon = Icons.Default.School
            )
        }

        // 2. Banner
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.keep_learning),
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (homeworkBySubject.isNotEmpty()) 
                                    "${homeworkBySubject.size} assignments pending" 
                                else stringResource(R.string.all_caught_up),
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2.5 Absence Alerts
        item(span = { GridItemSpan(2) }) {
            AbsenceAlertList(studentUid = uid)
        }

        // 3. Timetable Section
        item(span = { GridItemSpan(2) }) {
            TimetableSection(
                entries = todayEntries,
                onViewAllClick = {
                    val intent = android.content.Intent(context, StudentTimetableActivity::class.java).apply {
                        putExtra("className", className)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 3.5 Homework Section
        item(span = { GridItemSpan(2) }) {
            HomeworkSection(homeworkBySubject = homeworkBySubject)
        }

        // 4. Section Title
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.my_dashboard),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 5. Tiles
        items(studentTiles, key = { it.id }) { tile ->
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                AnimatedDashboardTile(
                    tile = tile,
                    onClick = { onTileClick(tile.id) }
                )
            }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun getCurrentDate(): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d", java.util.Locale.getDefault())
    return java.time.LocalDate.now().format(formatter)
}

@Preview(showBackground = true)
@Composable
fun StudentDashboardPreview() {
    MarkMyDayTheme {
        StudentDashboard(
            onNotificationClick = {},
            onTileClick = {},
            onNavigate = {},
            navController = rememberNavController()
        )
    }
}
