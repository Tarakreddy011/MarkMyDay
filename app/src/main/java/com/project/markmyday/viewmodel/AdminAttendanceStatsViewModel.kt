package com.project.markmyday.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class StudentAttendanceStat(
    val uid: String,
    val studentId: String, // School assigned ID
    val studentName: String,
    val presentDays: Int,
    val absentDays: Int,
) {
    val totalDays: Int get() = presentDays + absentDays
    val attendancePercentage: Float get() = if (totalDays > 0) presentDays.toFloat() / totalDays else 0f
}

sealed class AttendanceStatsState {
    object Idle : AttendanceStatsState()
    object Loading : AttendanceStatsState()
    data class Success(val stats: List<StudentAttendanceStat>) : AttendanceStatsState()
    data class Error(val message: String) : AttendanceStatsState()
}

class AdminAttendanceStatsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _availableClasses = MutableStateFlow<List<String>>(emptyList())
    val availableClasses: StateFlow<List<String>> = _availableClasses.asStateFlow()

    private val _statsState = MutableStateFlow<AttendanceStatsState>(AttendanceStatsState.Idle)
    val statsState: StateFlow<AttendanceStatsState> = _statsState.asStateFlow()

    private val _isLoadingClasses = MutableStateFlow(value = false)
    val isLoadingClasses: StateFlow<Boolean> = _isLoadingClasses.asStateFlow()

    init {
        fetchAvailableClasses()
    }

    private fun fetchAvailableClasses() {
        viewModelScope.launch {
            _isLoadingClasses.value = true
            try {
                val snapshot = firestore.collection("timetable").get().await()
                val classes = snapshot.documents.asSequence().map { it.id }.sorted().toList()
                _availableClasses.value = classes
            } catch (_: Exception) {
                // Handle error
            } finally {
                _isLoadingClasses.value = false
            }
        }
    }

    fun fetchStatsForClass(className: String) {
        viewModelScope.launch {
            _statsState.value = AttendanceStatsState.Loading
            try {
                val snapshot = firestore.collection("attendance")
                    .whereEqualTo("class_section", className)
                    .get()
                    .await()

                val presentCounts = mutableMapOf<String, Int>()
                val absentCounts = mutableMapOf<String, Int>()
                val allStudentIds = mutableSetOf<String>()

                for (doc in snapshot.documents) {
                    val present = (doc["present_students"] as? List<*>) ?: emptyList<String>()
                    val absent = (doc["absent_students"] as? List<*>) ?: emptyList<String>()

                    present.filterIsInstance<String>().forEach { id ->
                        presentCounts[id] = (presentCounts[id] ?: 0) + 1
                        allStudentIds.add(id)
                    }
                    absent.filterIsInstance<String>().forEach { id ->
                        absentCounts[id] = (absentCounts[id] ?: 0) + 1
                        allStudentIds.add(id)
                    }
                }

                val infoMap = mutableMapOf<String, Pair<String, String>>() // UID -> Pair(Name, SchoolID)
                val studentIdList = allStudentIds.toList()
                
                // Firestore whereIn supports up to 30 elements
                val chunks = studentIdList.chunked(size = 30)
                for (chunk in chunks) {
                    val userSnapshot = firestore.collection("users")
                        .whereIn("uid", chunk)
                        .get()
                        .await()
                    
                    for (userDoc in userSnapshot.documents) {
                        val name = userDoc.getString("name") ?: "Unknown"
                        val schoolId = userDoc.getString("studentId") ?: "N/A"
                        infoMap[userDoc.id] = Pair(name, schoolId)
                    }
                }

                val stats = allStudentIds.map { uid ->
                    val info = infoMap[uid]
                    StudentAttendanceStat(
                        uid = uid,
                        studentId = info?.second ?: "N/A",
                        studentName = info?.first ?: "Unknown",
                        presentDays = presentCounts[uid] ?: 0,
                        absentDays = absentCounts[uid] ?: 0,
                    )
                }.sortedBy { it.studentName }

                _statsState.value = AttendanceStatsState.Success(stats)
            } catch (e: Exception) {
                _statsState.value = AttendanceStatsState.Error(e.localizedMessage ?: "Failed to fetch attendance records")
            }
        }
    }

    fun resetStatsState() {
        _statsState.value = AttendanceStatsState.Idle
    }
}
