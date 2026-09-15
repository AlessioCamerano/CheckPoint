package com.example.checkpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.util.TimeUtils

/**
 * Rappresentazione di un singolo post.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostItem(
    post: Post,
    author: UserEntity? = null,
    tags: List<PostTagEntity> = emptyList(),
    isLiked: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onAuthorClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {}
) {
    val timeAgo = TimeUtils.getTimeAgo(post.timestamp)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall)
            .clickable { author?.let { onPostClick(PostWithAuthor(post, it, tags, isLiked)) } },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        ) {
            // Header: Autore e Tempo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = author != null) { 
                    author?.let { onAuthorClick(it) } 
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (author?.profilePictureUri != null) {
                        AsyncImage(
                            model = author.profilePictureUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
                Column {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

            // Corpo: Titolo
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // MEDIA SECTION
            if (post.mediaUri != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                when (post.mediaType) {
                    MediaType.IMAGE -> {
                        AsyncImage(
                            model = post.mediaUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )
                    }
                    MediaType.VIDEO -> {
                        VideoPlayer(
                            videoUri = post.mediaUri,
                            isMuted = post.isMuted
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Messaggio
            Text(
                text = post.initialMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // tags
            if (tags.isNotEmpty()) {
                val maxVisibleTags = 3
                val visibleTags = tags.take(maxVisibleTags)
                val remainingCount = tags.size - maxVisibleTags

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    visibleTags.forEach { tagEntity ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, 
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "#${tagEntity.tag}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (remainingCount > 0) {
                        Text(
                            text = "+$remainingCount...",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Footer: Interazioni (Flag per i preferiti)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Flag else Icons.Outlined.Flag,
                        contentDescription = stringResource(com.example.checkpoint.R.string.action_favorite),
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "${post.favoriteCount}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Post Item Dark")
@Composable
fun PostItemDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PostItem(
                post = Post(
                    id = "1",
                    authorName = "User Test 1",
                    authorEmail = "user1@gmail.com",
                    title = "My first checkpoint!",
                    initialMessage = "This is a sample message for the preview.",
                    favoriteCount = 12,
                    isFavorite = true,
                    timestamp = System.currentTimeMillis() - 7200000,
                    mediaUri = "https://example.com/image.jpg",
                    mediaType = MediaType.IMAGE
                ),
                tags = listOf(
                    PostTagEntity("1", "gaming"),
                    PostTagEntity("1", "coding")
                ),
                isLiked = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Post Item Light")
@Composable
fun PostItemLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PostItem(
                post = Post(
                    id = "1",
                    authorName = "User Test 1",
                    authorEmail = "user1@gmail.com",
                    title = "My first checkpoint!",
                    initialMessage = "This is a sample message for the preview.",
                    favoriteCount = 12,
                    isFavorite = false,
                    timestamp = System.currentTimeMillis() - 300000,
                    mediaUri = "https://example.com/image.jpg",
                    mediaType = MediaType.IMAGE
                ),
                tags = listOf(
                    PostTagEntity("1", "relax"),
                    PostTagEntity("1", "nature")
                ),
                isLiked = false
            )
        }
    }
}
