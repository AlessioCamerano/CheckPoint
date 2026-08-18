package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.ui.components.MainUiState
import com.example.checkpoint.ui.components.PostItem

/**
 * Contenuto della Homepage (Feed).
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onNavigateToNotifications: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // notifiche
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onNavigateToNotifications,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Titolo
        Text(
            text = "CheckPoint",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "take it easy, player... stay here and relax",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Feed",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Trending") },
                        onClick = { showSortMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Newest") },
                        onClick = { showSortMenu = false },
                        leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // feed
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.posts) { post ->
                PostItem(
                    post = post,
                    onFavoriteClick = { /* TODO */ }
                )
            }
            
            // fine feed
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isLoading) "Loading checkpoints..." else "No more checkpoints.",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}
