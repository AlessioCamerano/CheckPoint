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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing

/**
 * Schermata principale del feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onRefresh: () -> Unit = {},
    onSortOrderChange: (PostSortOrder) -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onUserClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Scorrimento automatico verso l'alto
    LaunchedEffect(state.sortOrder) {
        if (state.posts.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.medium)
        ) {
            // notifiche
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            
            // Titolo
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.main_relax_quote),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.small),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.main_feed_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.action_sort),
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
                                    text = stringResource(R.string.main_sort_trending),
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
                                    text = stringResource(R.string.main_sort_newest),
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

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Progress bar per feedback visivo
            if (state.isLoading && !state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.spacing.extraExtraSmall)
                        .padding(horizontal = MaterialTheme.spacing.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraExtraSmall))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // feed
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                items(
                    items = state.posts,
                    key = { it.post.id }
                ) { postWithAuthor ->
                    PostItem(
                        post = postWithAuthor.post,
                        author = postWithAuthor.author,
                        tags = postWithAuthor.tags,
                        isLiked = postWithAuthor.isLikedByCurrentUser,
                        favoriteCount = postWithAuthor.favoriteCount,
                        onFavoriteClick = { onToggleLike(postWithAuthor.post.id) },
                        onAuthorClick = onUserClick,
                        onPostClick = onPostClick
                    )
                }
                
                // fine feed
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isLoading) stringResource(R.string.main_loading) else stringResource(
                                R.string.main_no_more),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium,
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
    val author = UserEntity("user1@gmail.com", "User Test 1")
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = MainUiState(
                    posts = listOf(
                        PostWithAuthor(
                            post = PostEntity("1", "user1@gmail.com", "Hello!", "Welcome to the dark side.", System.currentTimeMillis(), "https://example.com/dark.jpg", MediaType.IMAGE),
                            author = author,
                            isLikedByCurrentUser = true,
                            favoriteCount = 10
                        ),
                        PostWithAuthor(
                            post = PostEntity("2", "user2@gmail.com", "Morning", "Coffee is ready.", System.currentTimeMillis()),
                            author = UserEntity("user2@gmail.com", "User Test 2"),
                            favoriteCount = 5
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
    val author = UserEntity("user1@gmail.com", "User Test 1")
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = MainUiState(
                    posts = listOf(
                        PostWithAuthor(
                            post = PostEntity("1", "user1@gmail.com", "Hello!", "Welcome to the light side.", System.currentTimeMillis(), "https://example.com/light.jpg", MediaType.IMAGE),
                            author = author,
                            isLikedByCurrentUser = true,
                            favoriteCount = 10
                        ),
                        PostWithAuthor(
                            post = PostEntity("2", "user2@gmail.com", "Morning", "Coffee is ready.", System.currentTimeMillis()),
                            author = UserEntity("user2@gmail.com", "User Test 2"),
                            favoriteCount = 5
                        )
                    ),
                    isDarkTheme = false
                )
            )
        }
    }
}
