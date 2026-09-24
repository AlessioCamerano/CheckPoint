package com.example.checkpoint.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkpoint.R
import com.example.checkpoint.ui.components.SupportDialog
import com.example.checkpoint.ui.state.AuthUiState
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.ui.viewmodel.AuthViewModel
import com.example.checkpoint.util.GoogleSignInHelper
import kotlinx.coroutines.launch

/**
 * Schermata di accesso (Login) tramite Email/Password o Google Sign-In.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleSignInHelper = remember { GoogleSignInHelper(context) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onLogin = { id, pass -> viewModel.onLogin(id, pass) },
        onGoogleLogin = {
            coroutineScope.launch {
                val idToken = googleSignInHelper.signIn()
                if (idToken != null) {
                    viewModel.onGoogleSignIn(idToken)
                }
            }
        },
        onNavigateToSignup = onNavigateToSignup,
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ------------------------------------------------------------------------------------------------
                // Intestazione: Titolo e Messaggio di Benvenuto
                // ------------------------------------------------------------------------------------------------
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.displayLarge,
                )

                Text(
                    text = stringResource(R.string.login_welcome),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.large),
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.medium),
                    )
                }


                // ------------------------------------------------------------------------------------------------
                // Form di Login (Email/Username e Password)
                // ------------------------------------------------------------------------------------------------
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(stringResource(R.string.field_login_identifier)) },
                    placeholder = { Text(stringResource(R.string.field_login_identifier)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    enabled = !uiState.isLoading,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.field_password)) },
                    placeholder = { Text(stringResource(R.string.field_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !uiState.isLoading,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))


                // ------------------------------------------------------------------------------------------------
                // Pulsanti di Accesso
                // ------------------------------------------------------------------------------------------------
                Button(
                    onClick = { onLogin(identifier, password) },
                    modifier = Modifier.fillMaxWidth().height(MaterialTheme.spacing.extraLarge),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.extraLarge,
                    enabled = !uiState.isLoading && identifier.isNotBlank() && password.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.login_button), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = stringResource(R.string.login_signup_hint),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) { onNavigateToSignup() },
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Text(
                        text = stringResource(R.string.login_forgot_password),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showSupportDialog = true },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface)
                    Text(
                        text = stringResource(R.string.login_or_divider),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface)
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                OutlinedButton(
                    onClick = onGoogleLogin,
                    modifier = Modifier.fillMaxWidth().height(MaterialTheme.spacing.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp),
                    enabled = !uiState.isLoading,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
                        Text(text = stringResource(R.string.login_google_button), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Login Dark")
@Composable
fun LoginScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        LoginScreenContent(
            uiState = AuthUiState(),
            onLogin = { _, _ -> },
            onGoogleLogin = {},
            onNavigateToSignup = {},
        )
    }
}

@Preview(showSystemUi = true, name = "Login Light")
@Composable
fun LoginScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        LoginScreenContent(
            uiState = AuthUiState(),
            onLogin = { _, _ -> },
            onGoogleLogin = {},
            onNavigateToSignup = {},
        )
    }
}
