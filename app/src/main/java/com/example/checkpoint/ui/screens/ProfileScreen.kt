package com.example.checkpoint.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.ui.components.SupportDialog
import com.example.checkpoint.ui.state.UserStatus
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = true,
    userStatus: UserStatus = UserStatus.Online,
    username: String = "Username",
    profilePictureUri: String? = null,
    onStatusChange: (UserStatus) -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showSupportDialog by remember { mutableStateOf(false) }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Header con Avatar e Status
        ProfileHeader(userStatus, username, profilePictureUri, onStatusChange)

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
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                SectionLabel(stringResource(com.example.checkpoint.R.string.profile_section_app))
                ThemeSwitchMenuItem(isDarkTheme, onThemeChange)
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
fun ProfileHeader(
    userStatus: UserStatus,
    username: String,
    profilePictureUri: String?,
    onStatusChange: (UserStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar con status
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri != null) {
                    AsyncImage(
                        model = profilePictureUri,
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
            
            // Pallino status
            Box(modifier = Modifier.wrapContentSize()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(MaterialTheme.spacing.extraSmall)
                        .clickable { showStatusMenu = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(userStatus.color)
                    )
                }

                // Menù a tendina
                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false },
                    // Offset per farlo apparire a destra (specchiato)
                    offset = androidx.compose.ui.unit.DpOffset(x = MaterialTheme.spacing.medium, y = 0.dp)
                ) {
                    UserStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onStatusChange(status)
                                showStatusMenu = false
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(MaterialTheme.spacing.smallMedium)
                                        .clip(CircleShape)
                                        .background(status.color)
                                )
                            }
                        )
                    }
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
