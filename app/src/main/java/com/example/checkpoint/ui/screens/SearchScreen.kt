package com.example.checkpoint.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.theme.CheckPointTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: MainUiState,
    onSearch: (String) -> Unit = {},
    onSaveSearch: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onUserClick: (UserEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearched by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Checkpoints", "Tags")
    val focusManager = LocalFocusManager.current

    val topPadding by animateDpAsState(
        targetValue = if (isSearched) 16.dp else 100.dp,
        animationSpec = tween(durationMillis = 600),
        label = "searchBarPosition"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            AnimatedVisibility(
                visible = !isSearched,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Checkpoint Search",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.isBlank()) isSearched = false
                    else {
                        onSearch(it)
                        isSearched = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search users, checkpoints or tags...", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            isSearched = true
                            onSearch(searchQuery)
                            onSaveSearch(searchQuery) // Save only on explicit action
                            focusManager.clearFocus()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSearched) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Ricerche recenti
                    if (state.searchHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onClearHistory) {
                                Text("Clear", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.searchHistory.reversed().forEach { query ->
                                AssistChip(
                                    onClick = {
                                        searchQuery = query
                                        isSearched = true
                                        onSearch(query)
                                    },
                                    label = { Text(query) },
                                    leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Trending Tags
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trending Now",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.trendingTags.forEach { tag ->
                            TagItem(tag = tag) {
                                searchQuery = tag
                                isSearched = true
                                onSearch(tag)
                                onSaveSearch(tag) // Save trending tag click
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
            } else {
                // Search Tabs
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Users
                            items(state.searchResultsUsers) { user ->
                                UserSearchResultItem(user, onClick = onUserClick)
                            }
                        }
                        1 -> { // Checkpoints
                            items(state.searchResultsPosts) { postWithAuthor ->
                                PostItem(
                                    post = postWithAuthor.post,
                                    author = postWithAuthor.author,
                                    tags = postWithAuthor.tags,
                                    isLiked = postWithAuthor.isLikedByCurrentUser,
                                    onFavoriteClick = { onToggleLike(postWithAuthor.post.id) },
                                    onAuthorClick = onUserClick
                                )
                            }
                        }
                        2 -> { // Tags
                            items(state.searchResultsTags) { postWithAuthor ->
                                PostItem(
                                    post = postWithAuthor.post,
                                    author = postWithAuthor.author,
                                    tags = postWithAuthor.tags,
                                    isLiked = postWithAuthor.isLikedByCurrentUser,
                                    onFavoriteClick = { onToggleLike(postWithAuthor.post.id) },
                                    onAuthorClick = onUserClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    user: UserEntity,
    onClick: (UserEntity) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(user) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.profilePictureUri != null) {
                AsyncImage(
                    model = user.profilePictureUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = user.email,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun TagItem(tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showSystemUi = true, name = "Search Dark")
@Composable
fun SearchScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SearchScreen(state = MainUiState(isDarkTheme = true))
        }
    }
}
