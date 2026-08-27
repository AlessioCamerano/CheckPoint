package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.theme.CheckPointTheme

/**
 * Contenuto della Homepage (Feed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onRefresh: () -> Unit = {},
    onSortOrderChange: (PostSortOrder) -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Scorrimento automatico verso l'alto
    LaunchedEffect(state.sortOrder) {
        listState.animateScrollToItem(0)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
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
                        tint = MaterialTheme.colorScheme.onSurface
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
                            text = { 
                                Text(
                                    text = "Trending",
                                    fontWeight = if (state.sortOrder == PostSortOrder.TRENDING) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = { 
                                onSortOrderChange(PostSortOrder.TRENDING)
                                showSortMenu = false 
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Newest",
                                    fontWeight = if (state.sortOrder == PostSortOrder.NEWEST) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = { 
                                onSortOrderChange(PostSortOrder.NEWEST)
                                showSortMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar per feedback visivo
            if (state.isLoading && !state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // feed
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.posts,
                    key = { it.post.id }
                ) { postWithAuthor ->
                    PostItem(
                        post = postWithAuthor.post,
                        authorProfilePicture = postWithAuthor.author.profilePictureUri,
                        tags = postWithAuthor.tags,
                        isLiked = postWithAuthor.isLikedByCurrentUser,
                        onFavoriteClick = { onToggleLike(postWithAuthor.post.id) }
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
}

@Preview(showSystemUi = true, name = "Home Feed Dark")
@Composable
fun MainScreenDarkPreview() {
    val author = UserEntity("user1@gmail.com", "User Test 1", "pass")
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = MainUiState(
                    posts = listOf(
                        PostWithAuthor(
                            post = Post("1", "User Test 1", "user1@gmail.com", "Hello!", "Welcome to the dark side.", true, 10, System.currentTimeMillis(), "https://example.com/dark.jpg", MediaType.IMAGE),
                            author = author,
                            isLikedByCurrentUser = true
                        ),
                        PostWithAuthor(
                            post = Post("2", "User Test 2", "user2@gmail.com", "Morning", "Coffee is ready.", false, 5, System.currentTimeMillis()),
                            author = UserEntity("user2@gmail.com", "User Test 2", "pass")
                        )
                    ),
                    isDarkTheme = true
                )
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Home Feed Light")
@Composable
fun MainScreenLightPreview() {
    val author = UserEntity("user1@gmail.com", "User Test 1", "pass")
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = MainUiState(
                    posts = listOf(
                        PostWithAuthor(
                            post = Post("1", "User Test 1", "user1@gmail.com", "Hello!", "Welcome to the light side.", true, 10, System.currentTimeMillis(), "https://example.com/light.jpg", MediaType.IMAGE),
                            author = author,
                            isLikedByCurrentUser = true
                        ),
                        PostWithAuthor(
                            post = Post("2", "User Test 2", "user2@gmail.com", "Morning", "Coffee is ready.", false, 5, System.currentTimeMillis()),
                            author = UserEntity("user2@gmail.com", "User Test 2", "pass")
                        )
                    ),
                    isDarkTheme = false
                )
            )
        }
    }
}
