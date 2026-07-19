package com.project.markmyday.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.project.markmyday.R
import com.project.markmyday.data.model.Teacher
import com.project.markmyday.ui.components.DashboardTopBar
import com.project.markmyday.ui.components.SearchBar
import com.project.markmyday.viewmodel.TeacherRegistrationState
import com.project.markmyday.ui.theme.MarkMyDayTheme
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    teachers: List<Teacher>,
    onEditTeacher: (Teacher) -> Unit,
    onDeleteTeacher: (Teacher) -> Unit,
    onBack: () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTeachers = remember(searchQuery, teachers) {
        if (searchQuery.isBlank()) {
            teachers
        } else {
            teachers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.teacherId.contains(searchQuery, ignoreCase = true) ||
                it.subject.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var teacherToEdit by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }

    if (teacherToDelete != null) {
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text(stringResource(R.string.delete_staff)) },
            text = { Text(stringResource(R.string.delete_confirmation, teacherToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        teacherToDelete?.let { onDeleteTeacher(it) }
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (teacherToEdit != null) {
        var editState by remember { 
            mutableStateOf(
                AddStaffFormState(
                    name = teacherToEdit?.name ?: "",
                    age = teacherToEdit?.age?.toString() ?: "",
                    dob = teacherToEdit?.dob ?: "",
                    gender = teacherToEdit?.gender ?: "",
                    phone = teacherToEdit?.phone ?: "",
                    email = teacherToEdit?.email ?: "",
                    subject = teacherToEdit?.subject ?: "",
                    homeSection = teacherToEdit?.homeSection ?: "",
                    classesTaughtCategories = teacherToEdit?.classesTaughtCategories ?: emptyList()
                )
            )
        }

        ModalBottomSheet(
            onDismissRequest = { teacherToEdit = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            AddStaffContent(
                title = stringResource(R.string.edit_staff_details),
                state = editState,
                onStateChange = { editState = it },
                onBack = { teacherToEdit = null },
                onSubmit = {
                    teacherToEdit?.let { original ->
                        onEditTeacher(
                            original.copy(
                                name = editState.name,
                                age = editState.age.toIntOrNull() ?: 0,
                                dob = editState.dob,
                                gender = editState.gender,
                                phone = editState.phone,
                                subject = editState.subject,
                                homeSection = editState.homeSection,
                                classesTaughtCategories = editState.classesTaughtCategories
                            )
                        )
                    }
                    teacherToEdit = null
                },
                registrationState = TeacherRegistrationState.Idle
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                title = stringResource(R.string.staff_list_title),
                onNotificationClick = onNotificationClick,
                icon = Icons.Default.People,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTeachers, key = { it.teacherId }) { teacher ->
                    TeacherListItem(
                        teacher = teacher,
                        onEdit = { teacherToEdit = teacher },
                        onDelete = { teacherToDelete = teacher }
                    )
                }
                
                if (filteredTeachers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_students_found, searchQuery),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherListItem(
    teacher: Teacher,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val initials = teacher.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Header: Avatar, Name, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar with Initials
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = teacher.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info Section: ID and Category Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoTag(
                    icon = Icons.Outlined.Badge,
                    text = teacher.teacherId,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                teacher.classesTaughtCategories.forEach { category ->
                    val color = when (category.lowercase()) {
                        "primary" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "secondary" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                        "high school" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    }
                    val textColor = when (category.lowercase()) {
                        "primary" -> Color(0xFF2E7D32)
                        "secondary" -> Color(0xFF1565C0)
                        "high school" -> Color(0xFFEF6C00)
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }

                    Surface(
                        color = color,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Contact and Personal Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = teacher.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${teacher.gender} • ${teacher.age} yrs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoTag(
    icon: ImageVector,
    text: String,
    containerColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StaffManagementScreenPreview() {
    val dummyTeachers = listOf(
        Teacher(teacherId = "T1261MAT", name = "Rahul Sharma", subject = "Maths"),
        Teacher(teacherId = "T1261SCI", name = "Priya Varma", subject = "Science"),
        Teacher(teacherId = "T1261ENG", name = "Amit Singh", subject = "English")
    )
    MarkMyDayTheme {
        StaffManagementScreen(
            teachers = dummyTeachers,
            onEditTeacher = {},
            onDeleteTeacher = {},
            onBack = {}
        )
    }
}
