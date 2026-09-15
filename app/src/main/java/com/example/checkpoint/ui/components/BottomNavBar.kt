package com.example.checkpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.ui.state.UserStatus
import com.example.checkpoint.ui.theme.spacing

@Composable
fun BottomNavBar(
    currentRoute: String?,
    userStatus: UserStatus = UserStatus.Online,
    profilePictureUri: String? = null,
    onNavigate: (String) -> Unit
) {
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = MaterialTheme.spacing.default
        ) {
            NavigationBarItem(
                selected = currentRoute == "search",
                onClick = { onNavigate("search") },
                icon = { Icon(Icons.Default.Search, contentDescription = stringResource(com.example.checkpoint.R.string.tab_users)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "add",
                onClick = { onNavigate("add") },
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(com.example.checkpoint.R.string.post_add_media)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "home_feed",
                onClick = { onNavigate("home_feed") },
                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(com.example.checkpoint.R.string.app_name)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "messages",
                onClick = { onNavigate("messages") },
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(com.example.checkpoint.R.string.messages_title)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "profile",
                onClick = { onNavigate("profile") },
                icon = { 
                    Box {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePictureUri != null) {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = stringResource(com.example.checkpoint.R.string.tab_profile),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(com.example.checkpoint.R.string.tab_profile),
                                    modifier = Modifier.size(20.dp),
                                    tint = if (currentRoute == "profile") 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        // Pallino stato in tempo reale
                        Box(
                            modifier = Modifier
                                .size(MaterialTheme.spacing.small)
                                .offset(x = 1.dp, y = 1.dp)
                                .clip(CircleShape)
                                .background(userStatus.color)
                                .align(Alignment.BottomEnd)
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
