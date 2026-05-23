package com.project.markmyday.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.viewmodel.AdminAttendanceStatsViewModel
import com.project.markmyday.viewmodel.AttendanceStatsState
import com.project.markmyday.viewmodel.StudentAttendanceStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceStatsScreen(
    onBack: () -> Unit,
    viewModel: AdminAttendanceStatsViewModel = viewModel(),
) {
    val availableClasses by viewModel.availableClasses.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val isLoadingClasses by viewModel.isLoadingClasses.collectAsState()

    var selectedClass by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedClass == null) "Select Class" else "Attendance: $selectedClass",
                        fontWeight = FontWeight.Bold,
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedClass != null) {
                            selectedClass = null
                            viewModel.resetStatsState()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedClass == null) {
                if (isLoadingClasses) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (availableClasses.isEmpty()) {
                    Text(
                        text = "No classes found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableClasses) { className ->
                            ClassCard(className = className) {
                                selectedClass = className
                                viewModel.fetchStatsForClass(className)
                            }
                        }
                    }
                }
            } else {
                when (statsState) {
                    is AttendanceStatsState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is AttendanceStatsState.Success -> {
                        val stats = (statsState as AttendanceStatsState.Success).stats
                        if (stats.isEmpty()) {
                            Text(
                                text = "No attendance records found for $selectedClass",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(stats) { stat ->
                                    StudentStatCard(stat = stat)
                                }
                            }
                        }
                    }
                    is AttendanceStatsState.Error -> {
                        Text(
                            text = (statsState as AttendanceStatsState.Error).message,
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun ClassCard(className: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Class,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = className,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StudentStatCard(stat: StudentAttendanceStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stat.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${stat.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attendance: ${(stat.attendancePercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stat.presentDays}P / ${stat.totalDays} Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { stat.attendancePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    stat.attendancePercentage >= 0.75f -> Color(0xFF4CAF50)
                    stat.attendancePercentage >= 0.50f -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
