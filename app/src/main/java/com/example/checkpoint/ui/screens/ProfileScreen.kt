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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.ui.components.SupportDialog
import com.example.checkpoint.ui.components.UserStatus

@Composable
fun ProfileScreen(
    userStatus: UserStatus = UserStatus.Online,
    onStatusChange: (UserStatus) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showSupportDialog by remember { mutableStateOf(false) }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header con Avatar e Status
        ProfileHeader(userStatus, onStatusChange)

        Spacer(modifier = Modifier.height(40.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SectionLabel("Account Settings")
                ProfileMenuItem(Icons.Default.Person, "Edit Profile")
                ProfileMenuItem(Icons.Default.Lock, "Change Password") {
                    showSupportDialog = true
                }
                ProfileMenuItem(Icons.Default.Group, "Friends")
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionLabel("App Settings")
                // Sezione vuota per ora
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                LogoutMenuItem(onLogout)
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userStatus: UserStatus,
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
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(65.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Pallino status
            Box(modifier = Modifier.wrapContentSize()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
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
                    offset = androidx.compose.ui.unit.DpOffset(x = 24.dp, y = 0.dp)
                ) {
                    UserStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label) },
                            onClick = {
                                onStatusChange(status)
                                showStatusMenu = false
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(status.color)
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Username",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
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
            color = Color.White.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
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
            color = Color.White.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                null,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Log Out",
                color = Color(0xFFEF5350),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
    }
}
