package com.project.markmyday.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.markmyday.data.model.UserProfile
import com.project.markmyday.utils.unsubscribeFromTopics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

val LocalSettingsViewModel = staticCompositionLocalOf<SettingsViewModel> {
    error("No SettingsViewModel provided")
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val profile: UserProfile) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _currentLanguage = MutableStateFlow(getCurrentLocale())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean("dark_mode", false)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        // Listen for auth state changes to fetch profile immediately upon login/session restore
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                fetchUserProfile()
            } else {
                _uiState.value = SettingsUiState.Error("User not authenticated.")
            }
        }

        // Apply saved dark mode on init
        val savedDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (savedDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val document = firestore.collection("users").document(uid).get().await()
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Unknown"
                        val role = document.getString("role") ?: "User"
                        val id = document.getString("studentId") ?: document.getString("teacherId") ?: "N/A"
                        val studentClass = document.getString("studentClass") ?: document.getString("home_section")
                        val email = document.getString("email")
                        val dob = document.getString("dob")
                        val password = document.getString("password")
                        
                        // New fields
                        val fatherName = document.getString("fatherName")
                        val motherName = document.getString("motherName")
                        val fatherPhone = document.getString("fatherPhone")
                        val motherPhone = document.getString("motherPhone")
                        val subject = document.getString("subject")
                        
                        val profile = UserProfile(
                            name = name,
                            role = role,
                            id = id,
                            studentClass = studentClass,
                            fatherName = fatherName,
                            motherName = motherName,
                            fatherPhone = fatherPhone,
                            motherPhone = motherPhone,
                            subject = subject,
                            email = email,
                            dob = dob,
                            password = password
                        )
                        _uiState.value = SettingsUiState.Success(profile)
                    } else {
                        _uiState.value = SettingsUiState.Error("User profile not found.")
                    }
                } else {
                    _uiState.value = SettingsUiState.Error("User not authenticated.")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "An error occurred.")
            }
        }
    }

    fun setLanguage(languageCode: String) {
        if (_currentLanguage.value == languageCode) return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        _currentLanguage.value = languageCode
        prefs.edit().putString("language_code", languageCode).apply()
    }

    fun toggleDarkMode(enabled: Boolean) {
        if (_isDarkMode.value == enabled) return
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun logout() {
        unsubscribeFromTopics()
        auth.signOut()
        
        // Clear auth cache to prevent stale data on next login
        getApplication<Application>().getSharedPreferences("auth_cache", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }

    private fun getCurrentLocale(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "en" else locales.toLanguageTags().split(",")[0].split("-")[0]
    }
}
