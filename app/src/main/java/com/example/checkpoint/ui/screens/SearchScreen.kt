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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing

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
    val tabs = listOf(
        stringResource(com.example.checkpoint.R.string.tab_users),
        stringResource(com.example.checkpoint.R.string.tab_checkpoints),
        stringResource(com.example.checkpoint.R.string.tab_tags)
    )
    val focusManager = LocalFocusManager.current

    val topPadding by animateDpAsState(
        targetValue = if (isSearched) MaterialTheme.spacing.medium else 100.dp,
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
                .padding(horizontal = MaterialTheme.spacing.medium),
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
                        text = stringResource(com.example.checkpoint.R.string.search_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
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
                placeholder = { Text(stringResource(com.example.checkpoint.R.string.search_placeholder), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
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

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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
                                text = stringResource(com.example.checkpoint.R.string.search_recent),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onClearHistory) {
                                Text(stringResource(com.example.checkpoint.R.string.search_clear), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            state.searchHistory.reversed().forEach { query ->
                                AssistChip(
                                    onClick = {
                                        searchQuery = query
                                        isSearched = true
                                        onSearch(query)
                                    },
                                    label = { Text(query, style = MaterialTheme.typography.labelMedium) },
                                    leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                                    shape = MaterialTheme.shapes.large
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    }

                    // Trending Tags
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(
                            text = stringResource(com.example.checkpoint.R.string.search_trending),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                // Search Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    when (selectedTab) {
                        0 -> { // Users
                            items(state.searchResultsUsers) { user ->
                                UserItem(user, onClick = onUserClick)
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
fun TagItem(tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.smallMedium, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.labelMedium,
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
