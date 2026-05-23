package com.project.markmyday

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.project.markmyday.ui.theme.MarkMyDayTheme
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import com.project.markmyday.ui.navigation.AppNavigation
import android.Manifest
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.project.markmyday.viewmodel.AuthViewModel
import com.project.markmyday.viewmodel.AuthResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.activity.result.contract.ActivityResultContracts

import com.project.markmyday.viewmodel.SettingsViewModel
import com.project.markmyday.viewmodel.LocalSettingsViewModel
import androidx.compose.runtime.CompositionLocalProvider

import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
        } else {
            Log.w("FCM", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for existing session
        authViewModel.checkSession()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val authState by authViewModel.authState.collectAsState()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            CompositionLocalProvider(LocalSettingsViewModel provides settingsViewModel) {
                MarkMyDayTheme(darkTheme = isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (authState) {
                            is AuthResult.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painter = painterResource(id = R.drawable.markmydayicon),
                                            contentDescription = "App Icon",
                                            modifier = Modifier.size(120.dp)
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "MarkMyDay",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            }
                            is AuthResult.Success -> {
                                val result = authState as AuthResult.Success
                                // Use Uri.encode instead of URLEncoder to handle spaces as %20 (which Compose Nav decodes automatically)
                                val encodedName = Uri.encode(result.name)
                                val encodedRole = Uri.encode(result.role)
                                
                                val initialDashboardRoute = when (result.role.lowercase()) {
                                    "principal", "headmaster", "admin" -> "admin_dashboard/$encodedName/$encodedRole"
                                    "teacher" -> {
                                        val section = Uri.encode(result.homeSection ?: "N/A")
                                        val subject = Uri.encode(result.subject ?: "N/A")
                                        "teacher_dashboard/$encodedName/$encodedRole/$section/$subject"
                                    }
                                    "student" -> {
                                        val studentId = Uri.encode(result.studentId ?: "N/A")
                                        val displayRole = if (result.homeSection != null && result.homeSection != "N/A") "Class ${result.homeSection}" else result.role
                                        val encodedDisplayRole = Uri.encode(displayRole)
                                        "student_dashboard/$encodedName/$encodedDisplayRole/$studentId/${result.uid}"
                                    }
                                    else -> {
                                        val studentId = Uri.encode(result.studentId ?: "N/A")
                                        val displayRole = if (result.homeSection != null && result.homeSection != "N/A") "Class ${result.homeSection}" else result.role
                                        val encodedDisplayRole = Uri.encode(displayRole)
                                        "student_dashboard/$encodedName/$encodedDisplayRole/$studentId/${result.uid}"
                                    }
                                }
                                AppNavigation(
                                    startDestination = initialDashboardRoute,
                                    initialDashboardRoute = initialDashboardRoute
                                )
                            }
                            else -> {
                                AppNavigation()
                            }
                        }
                    }
                }
            }
        }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log the token so you can copy it from Logcat
            Log.d("FCM", "FCM registration token: $token")
        }
    }
}
