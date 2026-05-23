package com.project.markmyday.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.project.markmyday.data.model.NotificationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val database by lazy { FirebaseDatabase.getInstance().getReference("notifications") }
    private val prefs = application.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    private var listener: ValueEventListener? = null

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _hasUnreadNotices = MutableStateFlow(false)
    val hasUnreadNotices = _hasUnreadNotices.asStateFlow()

    private var lastReadTimestamp: Long
        get() = prefs.getLong("last_read_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_read_timestamp", value).apply()

    fun fetchNotifications(role: String) {
        val decodedRole = try { java.net.URLDecoder.decode(role, "UTF-8") } catch (e: Exception) { role }
        
        // Remove old listener if exists
        listener?.let { database.removeEventListener(it) }

        Log.d("NotificationVM", "Fetching notifications for role: $decodedRole")

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val fetchedNotifications = withContext(Dispatchers.Default) {
                        Log.d("NotificationVM", "Data received. Count: ${snapshot.childrenCount}")
                        val list = mutableListOf<NotificationData>()
                        for (doc in snapshot.children) {
                            try {
                                if (doc.value is String) continue
                                
                                val item = doc.getValue(NotificationData::class.java)
                                if (item != null) {
                                    val audienceOriginal = item.audience
                                    val audienceLower = audienceOriginal.lowercase()
                                    val userRole = decodedRole.lowercase()
                                    val currentUid = auth.currentUser?.uid
                                    
                                    val show = when {
                                        userRole.contains("admin") || 
                                        userRole.contains("principal") || 
                                        userRole.contains("headmaster") -> true
                                        
                                        userRole.contains("teacher") -> 
                                            audienceLower == "all" || 
                                            audienceLower == "teachers" || 
                                            audienceLower == "teacher" || 
                                            audienceOriginal == currentUid
                                            
                                        else -> audienceLower == "all" || audienceOriginal == currentUid
                                    }

                                    if (show) {
                                        list.add(item)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("NotificationVM", "Error parsing notification: ${doc.key}", e)
                            }
                        }
                        list.sortedByDescending { it.timestamp }
                    }
                    
                    _notifications.value = fetchedNotifications
                    checkUnreadStatus(fetchedNotifications)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationVM", "Database error: ${error.message}")
            }
        }
        
        database.orderByChild("timestamp").addValueEventListener(listener!!)
    }

    private fun checkUnreadStatus(list: List<NotificationData>) {
        if (list.isEmpty()) {
            _hasUnreadNotices.value = false
            return
        }
        val newestTimestamp = list.first().timestamp / 1000
        _hasUnreadNotices.value = newestTimestamp > lastReadTimestamp
    }

    fun markAsRead() {
        val newestTimestamp = _notifications.value.firstOrNull()?.timestamp?.let { it / 1000 } ?: (System.currentTimeMillis() / 1000)
        lastReadTimestamp = newestTimestamp
        _hasUnreadNotices.value = false
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { database.removeEventListener(it) }
    }
}
