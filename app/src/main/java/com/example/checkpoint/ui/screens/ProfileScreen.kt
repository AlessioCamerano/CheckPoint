package com.example.checkpoint.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.ui.components.SupportDialog
import com.example.checkpoint.ui.state.UserStatus
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = true,
    isLocationVisible: Boolean = true,
    userStatus: UserStatus = UserStatus.Online,
    username: String = "Username",
    profilePictureUri: String? = null,
    onStatusChange: (UserStatus) -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {},
    onLocationVisibilityChange: (Boolean) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToMyCheckpoints: () -> Unit = {},
    onNavigateToLocalUsers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: ((Result<Unit>) -> Unit) -> Unit = {}
) {
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val reauthErrorMessage = stringResource(R.string.delete_account_reauth_required)
    val deleteAccountGenericError = stringResource(R.string.delete_account_error)

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text(text = stringResource(R.string.privacy_policy_title), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.privacy_policy_message)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }

    deleteAccountError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { deleteAccountError = null },
            title = { Text(text = stringResource(R.string.delete_account_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(text = errorMsg) },
            confirmButton = {
                TextButton(onClick = { deleteAccountError = null }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(text = stringResource(R.string.delete_account_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(text = stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount { result ->
                            result.onFailure { exc ->
                                val msg = if (exc is FirebaseAuthRecentLoginRequiredException) {
                                    reauthErrorMessage
                                } else {
                                    String.format(deleteAccountGenericError, exc.localizedMessage ?: "Unknown error")
                                }
                                deleteAccountError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.delete_account_button), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Header con Avatar
        ProfileHeader(username, profilePictureUri)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large + MaterialTheme.spacing.small))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large),
        ) {
            item {
                SectionLabel(stringResource(com.example.checkpoint.R.string.profile_section_account))
                ProfileMenuItem(Icons.Default.Person, stringResource(com.example.checkpoint.R.string.profile_menu_edit)) {
                    onNavigateToEditProfile()
                }
                ProfileMenuItem(Icons.Default.Lock, stringResource(com.example.checkpoint.R.string.profile_menu_password)) {
                    showSupportDialog = true
                }
                ProfileMenuItem(Icons.Default.Group, stringResource(com.example.checkpoint.R.string.profile_menu_follows)) {
                    onNavigateToFollowers()
                }
                ProfileMenuItem(Icons.Default.LocationOn, stringResource(com.example.checkpoint.R.string.profile_menu_find_local_users)) {
                    onNavigateToLocalUsers()
                }
                ProfileMenuItem(Icons.Default.PostAdd, stringResource(R.string.profile_my_checkpoints)) {
                    onNavigateToMyCheckpoints()
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                SectionLabel(stringResource(com.example.checkpoint.R.string.profile_section_app))
                ThemeSwitchMenuItem(isDarkTheme, onThemeChange)
                LocationVisibilitySwitchMenuItem(isLocationVisible, onLocationVisibilityChange)
                ProfileMenuItem(Icons.Default.PrivacyTip, stringResource(R.string.profile_menu_privacy_policy)) {
                    showPrivacyPolicyDialog = true
                }
                ProfileMenuItem(Icons.Default.Settings, stringResource(R.string.profile_menu_android_settings)) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                ProfileMenuItem(Icons.Default.Slideshow, stringResource(com.example.checkpoint.R.string.profile_menu_onboarding)) {
                    onNavigateToOnboarding()
                }
                MenuItemCaption(stringResource(com.example.checkpoint.R.string.profile_menu_onboarding_caption))
                ProfileMenuItem(Icons.Default.DeleteForever, stringResource(R.string.profile_menu_delete_account)) {
                    showDeleteAccountDialog = true
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large + MaterialTheme.spacing.smallMedium))
                LogoutMenuItem(onLogout)
            }
        }
    }
}

@Composable
fun ThemeSwitchMenuItem(
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.smallMedium, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(com.example.checkpoint.R.string.profile_menu_dark_mode),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isDark,
                onCheckedChange = onThemeChange,
                thumbContent = {
                    Icon(
                        imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun LocationVisibilitySwitchMenuItem(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.smallMedium, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVisible) Icons.Default.LocationOn else Icons.Default.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(R.string.profile_menu_location_visibility),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isVisible,
                onCheckedChange = onVisibilityChange,
                thumbContent = {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.LocationOn else Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun ProfileHeader(
    username: String,
    profilePictureUri: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!profilePictureUri.isNullOrBlank()) {
                    val avatarModel: Any? = remember(profilePictureUri) {
                        if (profilePictureUri.startsWith("data:")) {
                            try {
                                val base64Data = profilePictureUri.substringAfter("base64,")
                                Base64.decode(base64Data, Base64.DEFAULT)
                            } catch (_: Exception) {
                                profilePictureUri
                            }
                        } else {
                            profilePictureUri
                        }
                    }

                    AsyncImage(
                        model = avatarModel,
                        contentDescription = stringResource(com.example.checkpoint.R.string.profile_picture),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(65.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = username,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.smallMedium)
    )
}

@Composable
fun MenuItemCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.small,
            end = MaterialTheme.spacing.small,
            top = MaterialTheme.spacing.extraSmall,
            bottom = MaterialTheme.spacing.smallMedium
        )
    )
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.medium, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LogoutMenuItem(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLogout() }
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.medium, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
            Text(
                text = stringResource(com.example.checkpoint.R.string.profile_menu_logout),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
    }
}

@Preview(showSystemUi = true, name = "Profile Dark")
@Composable
fun ProfileScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen(
                isDarkTheme = true,
                userStatus = UserStatus.Online,
                username = "User Test 1"
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Profile Light")
@Composable
fun ProfileScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen(
                isDarkTheme = false,
                userStatus = UserStatus.Busy,
                username = "User Test 1"
            )
        }
    }
}
