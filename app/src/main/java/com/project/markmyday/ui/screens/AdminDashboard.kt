package com.project.markmyday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.R
import com.project.markmyday.ui.components.AnimatedDashboardTile
import com.project.markmyday.ui.components.DashboardBottomBar
import com.project.markmyday.ui.components.DashboardTopBar
import com.project.markmyday.ui.components.ProfileInitialsIcon
import com.project.markmyday.ui.components.SearchBar
import com.project.markmyday.ui.components.StatCard
import com.project.markmyday.ui.components.StatItem
import com.project.markmyday.ui.models.DashboardTile
import com.project.markmyday.ui.theme.MarkMyDayTheme
import com.project.markmyday.viewmodel.LeaveViewModel
import com.project.markmyday.viewmodel.NotificationViewModel
import com.project.markmyday.viewmodel.StudentViewModel
import com.project.markmyday.viewmodel.TeacherViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    userName: String = "Principal Sharma",
    userRole: String = "Administrator",
    onNotificationClick: () -> Unit,
    onTileClick: (String) -> Unit,
    onNavigate: (String) -> Unit,
    navController: NavHostController
) {
    val decodedName = remember(userName) { 
        try { java.net.URLDecoder.decode(userName, "UTF-8") } catch (e: Exception) { userName } 
    }
    val decodedRole = remember(userRole) { 
        try { java.net.URLDecoder.decode(userRole, "UTF-8") } catch (e: Exception) { userRole } 
    }

    val notificationViewModel: NotificationViewModel = viewModel()
    val teacherViewModel: TeacherViewModel = viewModel()
    val studentViewModel: StudentViewModel = viewModel()
    val leaveViewModel: LeaveViewModel = viewModel()

    val notifications by notificationViewModel.notifications.collectAsState()
    val teachers by teacherViewModel.allTeachers.collectAsState()
    val students by studentViewModel.allStudents.collectAsState()
    val leaves by leaveViewModel.allLeaves.collectAsState()

    val hasUnread by notificationViewModel.hasUnreadNotices.collectAsState()

    LaunchedEffect(userRole) {
        notificationViewModel.fetchNotifications(userRole)
    }

    var searchQuery by remember { mutableStateOf("") }

    val pendingLeavesCount = remember(leaves) {
        leaves.count { it.status == "pending" }
    }

    // Dynamic stats from ViewModels
    val staffLabel = stringResource(R.string.tile_staff_list)
    val studentsLabel = stringResource(R.string.tile_students)
    val leavesLabel = stringResource(R.string.tile_manage_leaves)
    val noticesLabel = stringResource(R.string.tile_notices)

    val stats = remember(teachers.size, students.size, pendingLeavesCount, notifications.size) {
        listOf(
            StatItem(Icons.Outlined.Badge, staffLabel, teachers.size, Color(0xFF4CAF50)),
            StatItem(Icons.Outlined.People, studentsLabel, students.size, Color(0xFF2196F3)),
            StatItem(Icons.Outlined.HistoryEdu, leavesLabel, pendingLeavesCount, Color(0xFFFF9800)),
            StatItem(Icons.Outlined.Campaign, noticesLabel, notifications.size, Color(0xFF9C27B0))
        )
    }

    val adminTiles = listOf(
        DashboardTile("notices", stringResource(R.string.tile_notices), Icons.Default.Campaign),
        DashboardTile("staff_management", stringResource(R.string.tile_staff_list), Icons.Default.Badge),
        DashboardTile("attendance_overview", stringResource(R.string.tile_daily_overview), Icons.AutoMirrored.Filled.FactCheck),
        DashboardTile("attendance_stats", "Attendance Stats", Icons.Default.AutoGraph),
        DashboardTile("attendance_reports", "Attendance Reports", Icons.Default.Assessment),
        DashboardTile("create_timetable", stringResource(R.string.tile_create_timetable), Icons.Default.CalendarToday),
        DashboardTile("add_staff", stringResource(R.string.tile_add_staff), Icons.Default.PersonAdd),
        DashboardTile("add_student", stringResource(R.string.tile_add_student), Icons.Default.PersonAddAlt1),
        DashboardTile("timetable", stringResource(R.string.tile_timetables), Icons.AutoMirrored.Filled.EventNote),
        DashboardTile("students", stringResource(R.string.tile_students), Icons.Default.People),
        DashboardTile("admissions", stringResource(R.string.tile_new_admission), Icons.Default.School),
        DashboardTile("leave", stringResource(R.string.tile_manage_leaves), Icons.Default.HistoryEdu),
        DashboardTile("updates", stringResource(R.string.tile_updates), Icons.Default.Update),
        DashboardTile("settings", stringResource(R.string.settings), Icons.Default.Settings),
        DashboardTile("reports", stringResource(R.string.nav_reports), Icons.Default.Assessment),
        DashboardTile("quiz_upload", stringResource(R.string.tile_upload_quiz), Icons.Default.Quiz),
        DashboardTile("course_manager", stringResource(R.string.tile_course_manager), Icons.Default.VideoLibrary)
    ).filter { it.label.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                icon = Icons.Default.AutoGraph,
                title = stringResource(R.string.app_name),
                onNotificationClick = onNotificationClick,
                notificationCount = if (hasUnread) 1 else 0,
                onProfileClick = { onTileClick("settings") }
            )
        },
        bottomBar = {
            DashboardBottomBar(
                currentRoute = "dashboard",
                onNavigate = onNavigate
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Welcome Header with Date
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hello, $decodedName",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$decodedRole • ${getCurrentDate()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        ProfileInitialsIcon(
                            name = decodedName,
                            size = 56.dp,
                            shape = CircleShape
                        )
                    }
                }
            }

            // Quick Stats Row
            items(stats) { stat ->
                Box(modifier = Modifier.padding(horizontal = if (stats.indexOf(stat) % 2 == 0) 12.dp else 0.dp, vertical = 0.dp)) {
                    // Small adjustment to padding to match the previous separate grid
                    StatCard(stat = stat)
                }
            }

            // Motivational Banner
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.manage_your_school),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.everything_in_one_place),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            // Section Title + Filter
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { /* Optional: extra filter action */ }) {
                        Icon(Icons.Default.Tune, contentDescription = "Filter")
                    }
                }
            }

            // Enhanced Search Bar
            item(span = { GridItemSpan(2) }) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // Modern Tile Grid
            items(adminTiles, key = { it.id }) { tile ->
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    AnimatedDashboardTile(
                        tile = tile,
                        onClick = { onTileClick(tile.id) }
                    )
                }
            }

            // Decorative Footer
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.made_for_futures),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarEnhanced(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(30.dp)),
        placeholder = { Text("Search modules...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(30.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        singleLine = true
    )
}

private fun getCurrentDate(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    return LocalDate.now().format(formatter)
}

// Previews
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AdminDashboardDarkPreview() {
    MarkMyDayTheme {
        AdminDashboard(
            onNotificationClick = {},
            onTileClick = {},
            onNavigate = {},
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    MarkMyDayTheme {
        AdminDashboard(
            onNotificationClick = {},
            onTileClick = {},
            onNavigate = {},
            navController = rememberNavController()
        )
    }
}
