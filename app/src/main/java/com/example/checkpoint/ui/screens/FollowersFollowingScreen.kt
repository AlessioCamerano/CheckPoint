package com.example.checkpoint.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.theme.CheckPointTheme

/**
 * Schermata per la consultazione degli elenchi dei follower e degli utenti seguiti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersFollowingScreen(
    followers: List<UserEntity>,
    following: List<UserEntity>,
    onBack: () -> Unit,
    onUserClick: (UserEntity) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_followers),
        stringResource(R.string.tab_following),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.follows_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ------------------------------------------------------------------------------------------------
            // Selettore Tab (Follower vs Seguiti)
            // ------------------------------------------------------------------------------------------------
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }


            // ------------------------------------------------------------------------------------------------
            // Elenco Utenti
            // ------------------------------------------------------------------------------------------------
            val currentList = if (selectedTab == 0) followers else following

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (currentList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (selectedTab == 0) stringResource(R.string.followers_empty) else stringResource(R.string.following_empty),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                fontStyle = FontStyle.Italic,
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
        UserEntity("u1@g.com", "User 1"),
        UserEntity("u2@g.com", "User 2"),
    )
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            FollowersFollowingScreen(
                followers = followers,
                following = emptyList(),
                onBack = {},
                onUserClick = {},
            )
        }
    }
}
