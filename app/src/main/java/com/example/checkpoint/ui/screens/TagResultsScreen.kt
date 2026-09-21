package com.example.checkpoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagResultsScreen(
    tag: String,
    posts: List<PostWithAuthor>,
    onBack: () -> Unit,
    onToggleLike: (String) -> Unit,
    onUserClick: (UserEntity) -> Unit,
    onPostClick: (PostWithAuthor) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                items(posts) { postWithAuthor ->
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
            }
        }
    }
}
