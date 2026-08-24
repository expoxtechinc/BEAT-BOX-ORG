package com.beatbox.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.R
import com.beatbox.ui.components.ErrorState
import com.beatbox.ui.theme.*

// =============================================================================
// LOGIN SCREEN
// =============================================================================

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AuthScaffold(
        title = "Welcome Back",
        subtitle = "Log in to your BeatBox account",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearLoginError() },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.error != null,
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearLoginError() },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.error != null,
            )

            // Error message
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Forgot password
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Forgot Password?")
            }

            // Login button
            Button(
                onClick = { viewModel.login(email.trim(), password) },
                enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Log In", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }

            // Register link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRegisterClick) {
                    Text("Sign Up", fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }
    }
}

// =============================================================================
// REGISTER SCREEN
// =============================================================================

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onRegisterSuccess()
    }

    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AuthScaffold(
        title = "Create Account",
        subtitle = "Join BeatBox and start sharing your music",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearRegisterError() },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase(); viewModel.clearRegisterError() },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Letters, numbers, and underscores only") },
            )

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name (optional)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearRegisterError() },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Min 8 chars, 1 uppercase, 1 lowercase, 1 number") },
            )

            // Error message
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Free upload notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandSuccess.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BrandSuccess)
                    Text(
                        text = "Music upload is always FREE on BeatBox",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandSuccess,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Register button
            Button(
                onClick = {
                    viewModel.register(
                        email.trim(),
                        password,
                        username.trim(),
                        displayName.ifBlank { null },
                    )
                },
                enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank() && username.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Sign Up", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }

            // Login link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onLoginClick) {
                    Text("Log In", fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }
    }
}

// =============================================================================
// FORGOT PASSWORD SCREEN
// =============================================================================

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onResetTokenReceived: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.forgotPasswordState.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            state.resetToken?.let { onResetTokenReceived(it) }
        }
    }

    AuthScaffold(
        title = "Forgot Password",
        subtitle = "Enter your email to receive a reset link",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearForgotPasswordError() },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (state.isSuccess) {
                Text(
                    text = "If an account exists with this email, a reset link has been sent.",
                    color = BrandSuccess,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { viewModel.forgotPassword(email.trim()) },
                enabled = !state.isLoading && email.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Login")
            }
        }
    }
}

// =============================================================================
// RESET PASSWORD SCREEN
// =============================================================================

@Composable
fun ResetPasswordScreen(
    token: String,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.resetPasswordState.collectAsStateWithLifecycle()

    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onResetSuccess()
    }

    val passwordsMatch = password == confirmPassword

    AuthScaffold(
        title = "Reset Password",
        subtitle = "Enter your new password",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearResetPasswordError() },
                label = { Text("New Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.error != null,
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; viewModel.clearResetPasswordError() },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                supportingText = {
                    if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                        Text("Passwords don't match", color = MaterialTheme.colorScheme.error)
                    }
                },
            )

            if (state.error != null) {
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.resetPassword(token, password) },
                enabled = !state.isLoading && password.isNotBlank() && passwordsMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Reset Password", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =============================================================================
// SHARED AUTH SCAFFOLD
// =============================================================================

@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BrandPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_beatbox_logo),
                contentDescription = "BeatBox",
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        content()
    }
}
