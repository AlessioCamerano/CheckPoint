package com.example.checkpoint.ui.screens

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.state.ChatUiState
import com.example.checkpoint.ui.state.FeedUiState
import com.example.checkpoint.ui.theme.spacing

@Composable
fun MessagesScreen(
    chatState: ChatUiState,
    feedState: FeedUiState,
    onPartnerClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onUserClick: (UserEntity) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_directs),
        stringResource(R.string.tab_saved)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(
            text = stringResource(R.string.messages_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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

        if (selectedTab == 0) {
            if (chatState.chatPartners.isEmpty()) {
                MessagesEmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    text = stringResource(R.string.messages_empty)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    items(chatState.chatPartners) { partner ->
                        UserItem(user = partner, onClick = onPartnerClick)
                    }
                }
            }
        } else {
            val savedPosts = feedState.posts.filter { it.isLikedByCurrentUser }

            if (savedPosts.isEmpty()) {
                MessagesEmptyState(
                    icon = Icons.Default.Flag,
                    text = stringResource(R.string.saves_empty)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    items(savedPosts) { postWithAuthor ->
                        CompactPostItem(
                            postWithAuthor = postWithAuthor,
                            onClick = { onPostClick(postWithAuthor) },
                            onToggleLike = onToggleLike,
                            onUserClick = onUserClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactPostItem(
    postWithAuthor: PostWithAuthor,
    onClick: () -> Unit,
    onToggleLike: (String) -> Unit,
    onUserClick: (UserEntity) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.smallMedium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val author = postWithAuthor.safeAuthor

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { onUserClick(author) },
                contentAlignment = Alignment.Center
            ) {
                if (!author.profilePictureUri.isNullOrBlank()) {
                    val avatarModel: Any? = remember(author.profilePictureUri) {
                        val uri = author.profilePictureUri
                        if (uri != null && uri.startsWith("data:")) {
                            try {
                                val base64Data = uri.substringAfter("base64,")
                                Base64.decode(base64Data, Base64.DEFAULT)
                            } catch (_: Exception) {
                                uri
                            }
                        } else {
                            uri
                        }
                    }
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = postWithAuthor.post.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.post_by_author, author.username.ifBlank { author.email }),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { onToggleLike(postWithAuthor.post.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MessagesEmptyState(icon: ImageVector, text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
        }
    }
}