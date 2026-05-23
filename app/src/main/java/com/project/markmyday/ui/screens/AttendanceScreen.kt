package com.project.markmyday.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.project.markmyday.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.ui.theme.MarkMyDayTheme
import com.project.markmyday.data.model.Student
import com.project.markmyday.viewmodel.AttendanceSubmissionState
import com.project.markmyday.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val assignedClasses by viewModel.assignedClasses.collectAsState()
    val studentsByClass by viewModel.studentsByClass.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()
    val presentStudentsList by viewModel.presentStudentsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    AttendanceContent(
        assignedClasses = assignedClasses,
        studentsByClass = studentsByClass,
        submissionState = submissionState,
        presentStudentsList = presentStudentsList,
        isLoading = isLoading,
        isEditMode = isEditMode,
        attendanceStates = viewModel.attendanceStates,
        onBack = onBack,
        onSubmitAttendance = { viewModel.submitAttendance(it) },
        onToggleAttendance = { uid, isPresent -> viewModel.toggleAttendance(uid, isPresent) },
        onResetSubmissionState = { viewModel.resetSubmissionState() },
        onClassSelected = { viewModel.checkExistingAttendance(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceContent(
    assignedClasses: List<String>,
    studentsByClass: Map<String, List<Student>>,
    submissionState: AttendanceSubmissionState,
    presentStudentsList: List<String>,
    isLoading: Boolean,
    isEditMode: Boolean,
    attendanceStates: Map<String, Boolean>,
    onBack: () -> Unit,
    onSubmitAttendance: (String) -> Unit,
    onToggleAttendance: (String, Boolean) -> Unit,
    onResetSubmissionState: () -> Unit,
    onClassSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var showSuccessDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val selectedClass = if (assignedClasses.isNotEmpty() && selectedTabIndex < assignedClasses.size) {
        assignedClasses[selectedTabIndex]
    } else ""
    val studentsInSelectedClass = studentsByClass[selectedClass] ?: emptyList()

    LaunchedEffect(selectedTabIndex) {
        if (selectedClass.isNotEmpty()) {
            onClassSelected(selectedClass)
        }
    }

    LaunchedEffect(submissionState) {
        when (submissionState) {
            is AttendanceSubmissionState.Success -> {
                showSuccessDialog = true
                onResetSubmissionState()
            }
            is AttendanceSubmissionState.Error -> {
                Toast.makeText(context, submissionState.message, Toast.LENGTH_LONG).show()
                onResetSubmissionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tile_mark_attendance), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            if (selectedClass.isNotEmpty()) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onSubmitAttendance(selectedClass) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            enabled = submissionState !is AttendanceSubmissionState.Loading
                        ) {
                            if (submissionState is AttendanceSubmissionState.Loading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = if (isEditMode) "Update Attendance" else stringResource(R.string.submit_attendance),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text(stringResource(R.string.attendance_success), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Present Students (${presentStudentsList.size}):", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(presentStudentsList) { name ->
                                Text("• $name", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSuccessDialog = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (assignedClasses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_classes_assigned))
                }
            } else {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    assignedClasses.forEachIndexed { index, className ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(className) }
                        )
                    }
                }

                if (studentsInSelectedClass.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_students_found, selectedClass))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(studentsInSelectedClass, key = { it.uid }) { student ->
                            StudentAttendanceRow(
                                student = student,
                                isPresent = attendanceStates[student.uid] ?: true,
                                onToggle = { onToggleAttendance(student.uid, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StudentAttendanceRow(
    student: Student,
    isPresent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ID: ${student.studentId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPresent) stringResource(R.string.present) else stringResource(R.string.absent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isPresent,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

