package com.example.checkpoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.theme.CheckPointTheme

/**
 * Schermata per visualizzare follower e persone seguite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersFollowingScreen(
    followers: List<UserEntity>,
    following: List<UserEntity>,
    onBack: () -> Unit,
    onUserClick: (UserEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Followers", "Following")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Followers & Following",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            val currentList = if (selectedTab == 0) followers else following

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == 0) "No followers yet." else "You are not following anyone.",
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                } else {
                    items(currentList) { user ->
                        UserItem(user = user, onClick = onUserClick)
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Followers Dark")
@Composable
fun FollowersFollowingScreenDarkPreview() {
    val followers = listOf(
        UserEntity("u1@g.com", "User 1", "pass"),
        UserEntity("u2@g.com", "User 2", "pass")
    )
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            FollowersFollowingScreen(
                followers = followers,
                following = emptyList(),
                onBack = {},
                onUserClick = {}
            )
        }
    }
}
