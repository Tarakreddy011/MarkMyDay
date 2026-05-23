package com.project.markmyday.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PersonalNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
)

class StudentNotificationViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private var listener: com.google.firebase.firestore.ListenerRegistration? = null

    private val _notifications = MutableStateFlow<List<PersonalNotification>>(emptyList())
    val notifications: StateFlow<List<PersonalNotification>> = _notifications.asStateFlow()

    fun startListening(studentUid: String) {
        if (studentUid.isEmpty()) return
        
        listener?.remove()

        // Get start of today (midnight) to refresh alerts daily
        val calendar = java.util.Calendar.getInstance()
        calendar[java.util.Calendar.HOUR_OF_DAY] = 0
        calendar[java.util.Calendar.MINUTE] = 0
        calendar[java.util.Calendar.SECOND] = 0
        calendar[java.util.Calendar.MILLISECOND] = 0
        val todayStart = com.google.firebase.Timestamp(calendar.time)
        
        listener = firestore.collection("users")
            .document(studentUid)
            .collection("notifications")
            .whereGreaterThanOrEqualTo("timestamp", todayStart)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        PersonalNotification(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }
                    _notifications.value = list
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
