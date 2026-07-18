package com.project.markmyday.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.project.markmyday.utils.subscribeUserToTopics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(
        val uid: String,
        val name: String,
        val role: String,
        val studentId: String? = null,
        val homeSection: String? = null,
        val subject: String? = null
    ) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("auth_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authState: StateFlow<AuthResult> = _authState.asStateFlow()

    fun loginUser(emailOrId: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                // Automatically append @gmail.com if it's just a user ID
                val email = if (emailOrId.contains("@")) emailOrId else "$emailOrId@gmail.com"

                // 1. Sign in with email and password
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid

                if (uid != null) {
                    fetchUserAndSetState(uid)
                } else {
                    _authState.value = AuthResult.Error("error_login_failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.localizedMessage ?: "error_unknown")
            }
        }
    }

    fun checkSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 1. Check cache first for immediate UI update
            val cachedUser = getCachedUser()
            if (cachedUser != null) {
                _authState.value = cachedUser
                Log.d("AuthViewModel", "Loaded user from cache: ${cachedUser.name}")
            } else {
                _authState.value = AuthResult.Loading
            }

            // 2. Fetch fresh data from Firestore in background
            viewModelScope.launch {
                fetchUserAndSetState(currentUser.uid)
            }
        } else {
            _authState.value = AuthResult.Idle
        }
    }

    private fun getCachedUser(): AuthResult.Success? {
        return try {
            val json = prefs.getString("user_data", null)
            if (json != null) {
                val user = gson.fromJson(json, AuthResult.Success::class.java)
                // Basic validation to ensure the cached object is well-formed
                if (user.name.isNotEmpty() && user.role.isNotEmpty()) {
                    user
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to parse cached user", e)
            null
        }
    }

    private fun saveUserToCache(user: AuthResult.Success) {
        try {
            val json = gson.toJson(user)
            prefs.edit().putString("user_data", json).apply()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to save user to cache", e)
        }
    }

    private suspend fun fetchUserAndSetState(uid: String) {
        try {
            // 2. Fetch user document from Firestore
            val document = firestore.collection("users").document(uid).get().await()

            if (document.exists()) {
                // 3. Extract name, role, and teacher specific fields
                val name = document.getString("name") ?: "User"
                val role = document.getString("role") ?: "unknown"
                
                // Handle naming inconsistencies across collections
                val homeSection = document.getString("homeSection") 
                    ?: document.getString("home_section") 
                    ?: document.getString("studentClass")

                val subject = document.getString("subject")
                val studentId = document.getString("studentId")

                Log.d("AuthViewModel", "User data fetched: Name=$name, Role=$role, Class=$homeSection")

                subscribeUserToTopics(role)

                val successResult = AuthResult.Success(uid, name, role, studentId, homeSection, subject)
                
                // Save to cache for next time
                saveUserToCache(successResult)
                
                _authState.value = successResult
            } else {
                _authState.value = AuthResult.Error("error_user_not_found")
            }
        } catch (e: Exception) {
            // If we have cached data, we might want to stay in Success state even if fetch fails
            if (_authState.value !is AuthResult.Success) {
                _authState.value = AuthResult.Error(e.localizedMessage ?: "error_unknown")
            }
        }
    }
}
