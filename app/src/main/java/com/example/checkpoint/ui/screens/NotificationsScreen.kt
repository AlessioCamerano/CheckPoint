package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.ui.theme.CheckPointTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification> = emptyList(),
    onBack: () -> Unit = {},
    onNotificationClick: (AppNotification) -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(com.example.checkpoint.R.string.notifications),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.example.checkpoint.R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            NotificationList(
                notifications = notifications,
                onNotificationClick = onNotificationClick
            )
        }
    }
}

@Composable
fun NotificationList(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit = {}
) {
    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(com.example.checkpoint.R.string.notifications_empty),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(notifications, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = { onNotificationClick(notification) }
                )
            }
        }
    }
}

@Composable
fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (notification.route) {
                "chat" -> Icons.Default.ChatBubbleOutline
                "post_detail" -> Icons.Default.ThumbUpAlt
                "home" -> Icons.Default.NotificationsNone
                else -> Icons.Default.PersonAddAlt1
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (notification.unread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notification.time,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Notifications Dark")
@Composable
fun NotificationsScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        NotificationsScreen(
            notifications = emptyList()
        )
    }
}

@Preview(showSystemUi = true, name = "Notifications Light")
@Composable
fun NotificationsScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        NotificationsScreen(
            notifications = emptyList()
        )
    }
}
