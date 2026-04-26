package com.topit.ecotrace.presentation.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.topit.ecotrace.ui.LocalAppStrings

@Composable
fun LoginScreen(
    contentPadding: PaddingValues,
    onBack: (() -> Unit)? = null,
    onOpenRegister: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (email: String, password: String) -> Unit = { _, _ -> },
) {
    val s = LocalAppStrings.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSubmit by remember(email, password, isLoading) {
        derivedStateOf { email.isValidEmail() && password.length >= 6 && !isLoading }
    }

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                icon = Icons.Default.Lock,
                title = s.loginTitle,
                subtitle = s.loginSubtitle,
                onBack = onBack,
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EcoSection(title = s.loginSectionTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text(s.authEmailLabel) },
                            placeholder = { Text(s.authEmailPlaceholder) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(s.authPasswordLabel) },
                            placeholder = { Text(s.authPasswordPlaceholder) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = { onLogin(email, password) },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(s.loginButton)
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenRegister,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(s.noAccountRegister)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    contentPadding: PaddingValues,
    onBack: (() -> Unit)? = null,
    onOpenLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRegister: (name: String, email: String, password: String) -> Unit = { _, _, _ -> },
) {
    val s = LocalAppStrings.current
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val canSubmit by remember(name, email, password, confirmPassword, isLoading) {
        derivedStateOf {
            name.isNotBlank() &&
                email.isValidEmail() &&
                password.length >= 6 &&
                password == confirmPassword &&
                !isLoading
        }
    }

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                icon = Icons.Default.PersonAdd,
                title = s.registerTitle,
                subtitle = s.registerSubtitle,
                onBack = onBack,
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EcoSection(title = s.registerSectionTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(s.authNameLabel) },
                            placeholder = { Text(s.authNamePlaceholder) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text(s.authEmailLabel) },
                            placeholder = { Text(s.authEmailPlaceholder) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(s.authPasswordLabel) },
                            placeholder = { Text(s.authPasswordPlaceholder) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(s.authConfirmPasswordLabel) },
                            placeholder = { Text(s.authConfirmPasswordPlaceholder) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (confirmPassword.isNotBlank() && password != confirmPassword) {
                            Text(
                                text = s.passwordsDontMatch,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = { onRegister(name.trim(), email, password) },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(s.registerButton)
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenLogin,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(s.haveAccountLogin)
                        }
                    }
                }
            }
        }
    }
}

private fun String.isValidEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
