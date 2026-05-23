package com.project.markmyday.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.markmyday.data.model.Student
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

sealed class AttendanceSubmissionState {
    object Idle : AttendanceSubmissionState()
    object Loading : AttendanceSubmissionState()
    object Success : AttendanceSubmissionState()
    data class Error(val message: String) : AttendanceSubmissionState()
}

class AttendanceViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ViewModel() {

    private val _assignedClasses = MutableStateFlow<List<String>>(emptyList())
    val assignedClasses: StateFlow<List<String>> = _assignedClasses.asStateFlow()

    private val _studentsByClass = MutableStateFlow<Map<String, List<Student>>>(emptyMap())
    val studentsByClass: StateFlow<Map<String, List<Student>>> = _studentsByClass.asStateFlow()

    private val _teacherId = MutableStateFlow("")
    private val _teacherSubject = MutableStateFlow("")

    val attendanceStates = mutableStateMapOf<String, Boolean>()

    private val _submissionState = MutableStateFlow<AttendanceSubmissionState>(AttendanceSubmissionState.Idle)
    val submissionState: StateFlow<AttendanceSubmissionState> = _submissionState.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isHomeTeacher = MutableStateFlow(false)
    val isHomeTeacher: StateFlow<Boolean> = _isHomeTeacher.asStateFlow()

    private val _presentStudentsList = MutableStateFlow<List<String>>(emptyList())
    val presentStudentsList: StateFlow<List<String>> = _presentStudentsList.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        loadTeacherData()
    }

    private fun loadTeacherData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val teacherDoc = firestore.collection("users").document(uid).get().await()
                val teacherId = teacherDoc.getString("teacherId") ?: ""
                val subject = teacherDoc.getString("subject") ?: ""

                _teacherSubject.value = subject
                _teacherId.value = teacherId

                checkHomeTeacherStatus(teacherId)
            } catch (e: Exception) {
                _isLoading.value = false
                _submissionState.value = AttendanceSubmissionState.Error(e.localizedMessage ?: "Failed to load teacher data")
            }
        }
    }

    private suspend fun checkHomeTeacherStatus(teacherId: String) {
        val dayOfWeek = LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        // Note: The timetable collection is indexed by className
        val timetables = firestore.collection("timetable").get().await()
        val assignedClasses = mutableListOf<String>()

        timetables.documents.forEach { doc ->
            val timetable = doc.toObject(com.project.markmyday.data.model.Timetable::class.java)
            if (timetable != null) {
                val todaySchedule = timetable.weeklySchedule[dayOfWeek]
                // Rule 1: First period teacher is Home Teacher for that day
                val firstPeriod = todaySchedule?.periods?.find { it.periodNumber == 1 }
                if (firstPeriod?.teacherId == teacherId) {
                    assignedClasses.add(timetable.className)
                }
            }
        }

        _assignedClasses.value = assignedClasses
        _isHomeTeacher.value = assignedClasses.isNotEmpty()

        if (assignedClasses.isNotEmpty()) {
            fetchStudents(assignedClasses)
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchStudents(assignments: List<String>) {
        val normalizedAssignments = assignments.map { it.replace("Class ", "").trim() }
        
        viewModelScope.launch {
            try {
                // Fetch approved leaves for today
                val today = com.google.firebase.Timestamp.now()
                val leavesSnapshot = firestore.collection("leaves")
                    .whereEqualTo("status", "approved")
                    .get()
                    .await()

                val studentsOnLeave = leavesSnapshot.documents.filter { doc ->
                    val start = doc.getTimestamp("startDate")
                    val end = doc.getTimestamp("endDate")
                    start != null && end != null && today >= start && today <= end
                }.mapNotNull { it.getString("studentId") }.toSet()

                firestore.collection("users")
                    .whereEqualTo("role", "student")
                    .whereIn("studentClass", normalizedAssignments)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _isLoading.value = false
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val students = snapshot.documents.map { doc ->
                                Student(
                                    uid = doc.getString("uid") ?: "",
                                    studentId = doc.getString("studentId") ?: "",
                                    name = doc.getString("name") ?: "",
                                    studentClass = doc.getString("studentClass") ?: "",
                                )
                            }

                            val grouped = students.groupBy { student ->
                                assignments.find { it.contains(student.studentClass) } ?: student.studentClass
                            }
                            _studentsByClass.value = grouped
                            
                            // Check for existing attendance for the first class in the list
                            if (assignments.isNotEmpty()) {
                                checkExistingAttendance(assignments[0])
                            } else {
                                students.forEach { student ->
                                    if (!attendanceStates.containsKey(student.uid)) {
                                        // Default to false if student is on leave, else true
                                        attendanceStates[student.uid] = !studentsOnLeave.contains(student.uid)
                                    }
                                }
                            }
                        }
                        _isLoading.value = false
                    }
            } catch (_: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun toggleAttendance(studentId: String, isPresent: Boolean) {
        attendanceStates[studentId] = isPresent
    }

    fun checkExistingAttendance(classSection: String) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val subject = _teacherSubject.value
        val docId = "${dateStr}_${classSection.replace(" ", "_")}_$subject"

        viewModelScope.launch {
            try {
                val doc = firestore.collection("attendance").document(docId).get().await()
                if (doc.exists()) {
                    _isEditMode.value = true
                    val presentUids = doc["present_students"] as? List<*> ?: emptyList<String>()
                    val absentUids = doc["absent_students"] as? List<*> ?: emptyList<String>()

                    val allStudentsInClass = _studentsByClass.value[classSection] ?: emptyList()
                    allStudentsInClass.forEach { student ->
                        if (presentUids.contains(student.uid)) {
                            attendanceStates[student.uid] = true
                        } else {
                            attendanceStates[student.uid] = !absentUids.contains(student.uid)
                        }
                    }
                } else {
                    _isEditMode.value = false
                    // Reset to default (Present unless on leave)
                    val allStudentsInClass = _studentsByClass.value[classSection] ?: emptyList()
                    
                    // Fetch leaves again or just use the logic from fetchStudents? 
                    // To keep it simple, if no doc exists, we default all to true.
                    // The fetchStudents already handles the leave logic, so we only reset if needed.
                    allStudentsInClass.forEach { student ->
                        if (!attendanceStates.containsKey(student.uid)) {
                            attendanceStates[student.uid] = true
                        }
                    }
                }
            } catch (e: Exception) {
                _isEditMode.value = false
            }
        }
    }

    fun submitAttendance(classSection: String) {
        val studentsInClass = _studentsByClass.value[classSection] ?: return
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val subject = _teacherSubject.value
        val docId = "${dateStr}_${classSection.replace(" ", "_")}_$subject"

        val presentStudentsUids = mutableListOf<String>()
        val absentStudentsUids = mutableListOf<String>()
        val presentStudentsNames = mutableListOf<String>()

        studentsInClass.forEach { student ->
            if (attendanceStates[student.uid] == true) {
                presentStudentsUids.add(student.uid)
                presentStudentsNames.add(student.name)
            } else {
                absentStudentsUids.add(student.uid)
            }
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val expiryDate = calendar.time

        val attendanceData = hashMapOf(
            "date" to com.google.firebase.Timestamp.now(),
            "expiry_date" to com.google.firebase.Timestamp(expiryDate),
            "class_section" to classSection,
            "subject" to subject,
            "teacher_id" to _teacherId.value,
            "present_students" to presentStudentsUids,
            "absent_students" to absentStudentsUids
        )

        viewModelScope.launch {
            _submissionState.value = AttendanceSubmissionState.Loading
            try {
                firestore.collection("attendance").document(docId).set(attendanceData).await()
                sendAbsenceNotifications(absentStudentsUids, subject, dateStr)
                _presentStudentsList.value = presentStudentsNames
                _submissionState.value = AttendanceSubmissionState.Success
            } catch (e: Exception) {
                _submissionState.value = AttendanceSubmissionState.Error(e.localizedMessage ?: "Submission failed")
            }
        }
    }

    private fun sendAbsenceNotifications(absentUids: List<String>, subject: String, date: String) {
        absentUids.forEach { uid ->
            val notificationData = hashMapOf(
                "title" to "Absence Alert",
                "message" to "You were marked absent for $subject on $date",
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .add(notificationData)
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = AttendanceSubmissionState.Idle
    }
}
