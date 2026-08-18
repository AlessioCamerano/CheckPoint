package com.example.checkpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.example.checkpoint.ui.screens.UserStatus

@Composable
fun BottomNavBar(
    currentRoute: String?,
    userStatus: UserStatus = UserStatus.Online,
    onNavigate: (String) -> Unit
) {
    Column {
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentRoute == "search",
                onClick = { onNavigate("search") },
                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "add",
                onClick = { onNavigate("add") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "home_feed",
                onClick = { onNavigate("home_feed") },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = currentRoute == "messages",
                onClick = { onNavigate("messages") },
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Messages") },
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
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                        // Pallino stato in tempo reale
                        Box(
                            modifier = Modifier
                                .size(8.dp)
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
