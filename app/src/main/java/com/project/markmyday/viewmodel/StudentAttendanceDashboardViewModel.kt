package com.project.markmyday.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import android.util.Log

data class AttendanceSummary(
    val presentDays: Int = 0,
    val absentDays: Int = 0,
)

data class MonthlyAttendance(
    val month: Int, // 1-12
    val presentDays: Int,
    val absentDays: Int,
)

sealed class StudentAttendanceState {
    object Loading : StudentAttendanceState()
    data class Success(
        val monthlySummary: AttendanceSummary,
        val yearlyData: List<MonthlyAttendance>,
    ) : StudentAttendanceState()
    data class Error(val message: String) : StudentAttendanceState()
}

class StudentAttendanceDashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<StudentAttendanceState>(StudentAttendanceState.Loading)
    val uiState: StateFlow<StudentAttendanceState> = _uiState.asStateFlow()

    fun fetchAttendanceData(studentId: String) {
        Log.d("AttendanceDashboardVM", "Fetching attendance data for: $studentId")
        viewModelScope.launch {
            _uiState.value = StudentAttendanceState.Loading
            try {
                // Fetch attendance documents where student is present
                val presentSnapshot = firestore.collection("attendance")
                    .whereArrayContains("present_students", studentId)
                    .get()
                    .await()

                // Fetch attendance documents where student is absent
                val absentSnapshot = firestore.collection("attendance")
                    .whereArrayContains("absent_students", studentId)
                    .get()
                    .await()
                
                Log.d("AttendanceDashboardVM", "Present docs: ${presentSnapshot.size()}, Absent docs: ${absentSnapshot.size()}")

                // If UID query returned nothing, maybe try with school studentId? 
                // We'll fetch student info first to get the school ID if needed.
                var schoolStudentId = ""
                try {
                    val userDoc = firestore.collection("users").document(studentId).get().await()
                    schoolStudentId = userDoc.getString("studentId") ?: ""
                    Log.d("AttendanceDashboardVM", "School Student ID: $schoolStudentId")
                } catch (e: Exception) {
                    Log.e("AttendanceDashboardVM", "Failed to fetch school ID", e)
                }

                val allDocs = if ((schoolStudentId.isNotEmpty()) && (schoolStudentId != studentId)) {
                    val presentSnapshot2 = firestore.collection("attendance")
                        .whereArrayContains("present_students", schoolStudentId)
                        .get()
                        .await()
                    val absentSnapshot2 = firestore.collection("attendance")
                        .whereArrayContains("absent_students", schoolStudentId)
                        .get()
                        .await()
                    Log.d("AttendanceDashboardVM", "Present docs (ID): ${presentSnapshot2.size()}, Absent docs (ID): ${absentSnapshot2.size()}")
                    (presentSnapshot.documents + absentSnapshot.documents + presentSnapshot2.documents + absentSnapshot2.documents)
                } else {
                    (presentSnapshot.documents + absentSnapshot.documents)
                }

                // Merge and sort in memory to avoid needing composite indexes for OR + orderBy
                val uniqueDocs = allDocs.distinctBy { it.id }
                    .sortedByDescending { it.getTimestamp("date") }

                Log.d("AttendanceDashboardVM", "Total unique docs: ${uniqueDocs.size}")

                val now = LocalDate.now()
                val currentMonth = now.monthValue
                val currentYear = now.year

                var currentMonthPresent = 0
                var currentMonthAbsent = 0

                val yearlyMap = mutableMapOf<Int, MonthlyAttendance>()
                // Initialize all months of the current year
                for (i in 1..12) {
                    yearlyMap[i] = MonthlyAttendance(i, 0, 0)
                }

                for (doc in uniqueDocs) {
                    val date = doc.getTimestamp("date")?.toDate()?.toInstant()
                        ?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: continue
                    
                    val presentList = (doc["present_students"] as? List<*>) ?: emptyList<String>()
                    // Check for both UID and school ID in present list
                    val isPresent = presentList.contains(studentId) || (schoolStudentId.isNotEmpty() && presentList.contains(schoolStudentId))
                    
                    // Yearly data (current year only for the graph)
                    if (date.year == currentYear) {
                        val month = date.monthValue
                        val current = yearlyMap[month] ?: MonthlyAttendance(month, 0, 0)
                        yearlyMap[month] = if (isPresent) {
                            current.copy(presentDays = current.presentDays + 1)
                        } else {
                            current.copy(absentDays = current.absentDays + 1)
                        }
                    }

                    // Current month summary
                    if (date.year == currentYear && date.monthValue == currentMonth) {
                        if (isPresent) currentMonthPresent++ else currentMonthAbsent++
                    }
                }

                _uiState.value = StudentAttendanceState.Success(
                    monthlySummary = AttendanceSummary(currentMonthPresent, currentMonthAbsent),
                    yearlyData = yearlyMap.values.sortedBy { it.month }
                )
            } catch (e: Exception) {
                Log.e("AttendanceDashboardVM", "Error processing attendance", e)
                _uiState.value = StudentAttendanceState.Error(e.localizedMessage ?: "Failed to fetch attendance")
            }
        }
    }
}
