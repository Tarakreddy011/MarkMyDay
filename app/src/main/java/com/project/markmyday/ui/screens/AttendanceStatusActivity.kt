package com.project.markmyday.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.markmyday.ui.theme.MarkMyDayTheme
import java.text.SimpleDateFormat
import java.util.*

class AttendanceStatusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkMyDayTheme {
                AttendanceStatusContainer(onFinish = { finish() })
            }
        }
    }
}

@Composable
fun AttendanceStatusContainer(onFinish: () -> Unit) {
    var attendanceStatus by remember { mutableStateOf<AttendanceState>(AttendanceState.Loading) }
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            attendanceStatus = AttendanceState.Error("User not authenticated")
            return@LaunchedEffect
        }

        // Support both teacher and student roles
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val teacherId = userDoc.getString("teacherId")
                val studentId = userDoc.getString("studentId")
                val targetCollection = when {
                    !teacherId.isNullOrEmpty() -> "teachers" to teacherId
                    !studentId.isNullOrEmpty() -> "students" to studentId
                    else -> null
                }

                if (targetCollection == null) {
                    attendanceStatus = AttendanceState.Error("User record not found")
                    return@addOnSuccessListener
                }

                val (collectionName, recordId) = targetCollection
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                firestore.collection(collectionName)
                    .document(recordId)
                    .collection("attendance_logs")
                    .document(today)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists() && document.getString("status") == "Present") {
                            val timestamp = document.getTimestamp("time")
                            val timeString = if (timestamp != null) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate())
                            } else {
                                "N/A"
                            }
                            attendanceStatus = AttendanceState.Present(timeString)
                        } else {
                            attendanceStatus = AttendanceState.Pending
                        }
                    }
                    .addOnFailureListener { e ->
                        attendanceStatus = AttendanceState.Error(e.localizedMessage ?: "Unknown error")
                    }
            }
            .addOnFailureListener { e ->
                attendanceStatus = AttendanceState.Error(e.localizedMessage ?: "Failed to fetch user data")
            }
    }

    AttendanceStatusScreen(state = attendanceStatus, onFinish = onFinish)
}

sealed class AttendanceState {
    object Loading : AttendanceState()
    object Pending : AttendanceState()
    data class Present(val scanTime: String) : AttendanceState()
    data class Error(val message: String) : AttendanceState()
}

@Composable
fun AttendanceStatusScreen(
    state: AttendanceState,
    onFinish: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                is AttendanceState.Loading -> {
                    CircularProgressIndicator()
                }
                is AttendanceState.Present -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Attendance Marked for Today",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scanned in at: ${state.scanTime}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is AttendanceState.Pending -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Attendance Pending",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is AttendanceState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onFinish,
                modifier = Modifier.width(200.dp)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}
