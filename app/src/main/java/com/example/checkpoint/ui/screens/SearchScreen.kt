package com.example.checkpoint.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.SearchUiState
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing

/**
 * Schermata di ricerca per utenti, post e tag con supporto alla cronologia ricerche ed alle tendenze.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onSearch: (String) -> Unit = {},
    onSaveSearch: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onUserClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onSortChange: (PostSortOrder) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearched by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }

    val tabs = listOf(
        stringResource(R.string.tab_users),
        stringResource(R.string.tab_checkpoints),
        stringResource(R.string.tab_tags),
    )

    val focusManager = LocalFocusManager.current
    val topPadding by animateDpAsState(
        targetValue = if (isSearched) MaterialTheme.spacing.medium else 100.dp,
        animationSpec = tween(durationMillis = 600),
        label = "searchBarPosition",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            AnimatedVisibility(
                visible = !isSearched,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.search_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                }
            }

            // ------------------------------------------------------------------------------------------------
            // Barra di Ricerca
            // ------------------------------------------------------------------------------------------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isBlank()) {
                        isSearched = false
                    } else {
                        onSearch(it)
                        isSearched = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            isSearched = true
                            onSearch(searchQuery)
                            onSaveSearch(searchQuery)
                            focusManager.clearFocus()
                        }
                    },
                ),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))


            // ------------------------------------------------------------------------------------------------
            // Cronologia / Tendenze o Risultati di Ricerca
            // ------------------------------------------------------------------------------------------------
            if (!isSearched) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.searchHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.search_recent),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = onClearHistory) {
                                Text(
                                    text = stringResource(R.string.search_clear),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            state.searchHistory.reversed().forEach { query ->
                                AssistChip(
                                    onClick = {
                                        searchQuery = query
                                        isSearched = true
                                        onSearch(query)
                                    },
                                    label = { Text(query, style = MaterialTheme.typography.labelMedium) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = MaterialTheme.shapes.large,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(
                            text = stringResource(R.string.search_trending),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        state.trendingTags.forEach { tag ->
                            TagItem(tag = tag) {
                                searchQuery = tag
                                isSearched = true
                                onSearch(tag)
                                onSaveSearch(tag)
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp,
                        divider = {},
                        modifier = Modifier.weight(1f),
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.main_sort_trending)) },
                                onClick = {
                                    onSortChange(PostSortOrder.TRENDING)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.main_sort_newest)) },
                                onClick = {
                                    onSortChange(PostSortOrder.NEWEST)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    when (selectedTab) {
                        0 -> {
                            val sortedUsers = when (state.searchSortOrder) {
                                PostSortOrder.NEWEST -> state.searchResultsUsers.sortedByDescending { it.username }
                                PostSortOrder.TRENDING -> state.searchResultsUsers.sortedBy { it.username }
                            }
                            items(sortedUsers) { user ->
                                UserItem(user, onClick = onUserClick)
                            }
                        }
                        1 -> {
                            val sortedPosts = when (state.searchSortOrder) {
                                PostSortOrder.NEWEST -> state.searchResultsPosts.sortedByDescending { it.post.timestamp }
                                PostSortOrder.TRENDING -> state.searchResultsPosts.sortedByDescending { it.favoriteCount }
                            }
                            items(sortedPosts) { postWithAuthor ->
                                PostItem(
                                    post = postWithAuthor.post,
                                    author = postWithAuthor.safeAuthor,
                                    tags = postWithAuthor.tags,
                                    isLiked = postWithAuthor.isLikedByCurrentUser,
                                    favoriteCount = postWithAuthor.favoriteCount,
                                    onFavoriteClick = { onToggleLike(postWithAuthor.post.id) },
                                    onAuthorClick = onUserClick,
                                    onPostClick = onPostClick,
                                )
                            }
                        }
                        2 -> {
                            val sortedTags = when (state.searchSortOrder) {
                                PostSortOrder.NEWEST -> state.searchResultsTags.sortedDescending()
                                PostSortOrder.TRENDING -> state.searchResultsTags.sorted()
                            }
                            items(sortedTags) { tag ->
                                TagSearchItem(tag = tag, onClick = { onTagClick(tag) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Elemento lista per la rappresentazione di un tag nei risultati di ricerca.
 */
@Composable
fun TagSearchItem(tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Chip o badge per la selezione di un tag popolare.
 */
@Composable
fun TagItem(tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.smallMedium, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showSystemUi = true, name = "Search Dark")
@Composable
fun SearchScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SearchScreen(state = SearchUiState())
        }
    }
}
