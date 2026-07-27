package com.project.markmyday.ui.auth

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.project.markmyday.R
import com.project.markmyday.viewmodel.AuthResult
import com.project.markmyday.viewmodel.AuthViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.project.markmyday.ui.theme.MarkMyDayTheme

enum class AuthState {
    PRE_LOGIN, LOGIN
}

@Composable
fun AuthenticationScreen(onLoginSuccess: (String, String, String?, String?, String?, String) -> Unit) {
    var authState by remember { mutableStateOf(AuthState.PRE_LOGIN) }

    AnimatedContent(
        targetState = authState,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "AuthTransition"
    ) { state ->
        when (state) {
            AuthState.PRE_LOGIN -> PreLoginContent(onStart = { authState = AuthState.LOGIN })
            AuthState.LOGIN -> LoginContent(
                onLogin = onLoginSuccess
            )
        }
    }
}

@Composable
fun PreLoginContent(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.preloginbackground),//have to change this screen
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 500f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.learning_adventure),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.start_learning), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LoginContent(
    onLogin: (String, String, String?, String?, String?, String) -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val authResult by viewModel.authState.collectAsState()

    LoginContentInternal(
        authResult = authResult,
        onLogin = onLogin,
        onLoginClick = { email, password -> viewModel.loginUser(email, password) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContentInternal(
    authResult: AuthResult,
    onLogin: (String, String, String?, String?, String?, String) -> Unit,
    onLoginClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isDarkMode = isSystemInDarkTheme()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authResult) {
        when (authResult) {
            is AuthResult.Success -> {
                onLogin(authResult.name, authResult.role, authResult.studentId, authResult.homeSection, authResult.subject, authResult.uid)
            }
            is AuthResult.Error -> {
                val errorKey = authResult.message
                val displayMessage = when(errorKey) {
                    "error_login_failed" -> context.getString(R.string.error_login_failed)
                    "error_user_not_found" -> context.getString(R.string.error_user_not_found)
                    "error_unknown" -> context.getString(R.string.error_unknown)
                    else -> errorKey
                }
                snackbarHostState.showSnackbar(displayMessage)
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.loginscreenbackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isDarkMode) 0.9f else 1f
        )

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode)
                            Color(0xFF1E1E2E).copy(alpha = 0.92f)
                        else Color.White.copy(alpha = 0.92f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 8.dp),
                    border = if (isDarkMode)
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated welcome text with gradient
                        val gradientColors = if (isDarkMode)
                            listOf(Color(0xFF9D4EDD), Color(0xFFFF6B6B))
                        else listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))

                        Text(
                            text = stringResource(R.string.welcome_back),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            ),
                            color = if (isDarkMode) Color.White else Color(0xFF1A1A2E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.sign_in_continue),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF666666)
                            )
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // User ID TextField
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.lowercase() },
                            label = { Text(stringResource(R.string.user_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkMode) Color.White else Color(0xFF1A1A2E),
                                unfocusedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF1A1A2E),
                                focusedBorderColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f),
                                focusedLabelColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray,
                                cursorColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password TextField with Animated Eye Icon
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff

                                    AnimatedContent(
                                        targetState = icon,
                                        transitionSpec = {
                                            scaleIn(animationSpec = tween(200)) togetherWith scaleOut(animationSpec = tween(200))
                                        },
                                        label = "EyeIconAnimation"
                                    ) { targetIcon ->
                                        Icon(
                                            imageVector = targetIcon,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF666666)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkMode) Color.White else Color(0xFF1A1A2E),
                                unfocusedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF1A1A2E),
                                focusedBorderColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f),
                                focusedLabelColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray,
                                cursorColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Forgot Password link
                        TextButton(
                            onClick = {
                                // Handle forgot password
                                Toast.makeText(context, context.getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(R.string.forgot_password),
                                color = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    onLoginClick(email, password)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary
                            ),
                            enabled = authResult !is AuthResult.Loading
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (authResult is AuthResult.Loading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(3.dp),
                                        color = if (isDarkMode) Color.White else Color.White,
                                        trackColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(R.string.logging_in), color = Color.White, fontSize = 16.sp)
                                } else {
                                    Text(stringResource(R.string.login_button), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign up suggestion
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.dont_know_details),
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF666666),
                                fontSize = 14.sp
                            )
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.forget_credentials),
                                    color = if (isDarkMode) Color(0xFF9D4EDD) else MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthenticationScreenPreview() {
    MarkMyDayTheme {
        AuthenticationScreen(onLoginSuccess = { _, _, _, _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun PreLoginContentPreview() {
    MarkMyDayTheme {
        PreLoginContent(onStart = {})
    }
}

@Preview(showBackground = true,uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginContentPreview() {
    MarkMyDayTheme {
        LoginContentInternal(
            authResult = AuthResult.Idle,
            onLogin = { _, _, _, _, _, _ -> },
            onLoginClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentLoadingPreview() {
    MarkMyDayTheme {
        LoginContentInternal(
            authResult = AuthResult.Loading,
            onLogin = { _, _, _, _, _, _ -> },
            onLoginClick = { _, _ -> }
        )
    }
}
