package com.example.checkpoint.ui.components

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.util.TimeUtils

/**
 * Componente UI per la rappresentazione visiva di un singolo post nella lista del Feed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostItem(
    post: PostEntity,
    author: UserEntity? = null,
    tags: List<PostTagEntity> = emptyList(),
    isLiked: Boolean = false,
    favoriteCount: Int = 0,
    onFavoriteClick: () -> Unit = {},
    onAuthorClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {},
) {
    val timeAgo = TimeUtils.getTimeAgo(post.timestamp)
    var showFullScreenImage by remember { mutableStateOf(false) }

    if (showFullScreenImage && !post.mediaUri.isNullOrBlank()) {
        FullScreenImageDialog(
            imageUri = post.mediaUri,
            onDismiss = { showFullScreenImage = false },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall)
            .clickable {
                onPostClick(PostWithAuthor(post, author ?: UserEntity(email = post.authorEmail), tags, isLiked, favoriteCount))
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        ) {
            // ------------------------------------------------------------------------------------------------
            // Intestazione: Foto Profilo, Autore e Tempo Trascorso
            // ------------------------------------------------------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = author != null) {
                    author?.let { onAuthorClick(it) }
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!author?.profilePictureUri.isNullOrBlank()) {
                        val avatarModel: Any? = remember(author.profilePictureUri) {
                            val uri = author.profilePictureUri
                            if (uri.startsWith("data:")) {
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
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
                Column {
                    Text(
                        text = author?.username ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))


            // ------------------------------------------------------------------------------------------------
            // Contenuto: Titolo e Media (Immagine/Video)
            // ------------------------------------------------------------------------------------------------
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            if (!post.mediaUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                val imageModel: Any? = remember(post.mediaUri) {
                    val uri = post.mediaUri
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
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { showFullScreenImage = true },
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Text(
                text = post.initialMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )


            // ------------------------------------------------------------------------------------------------
            // Sezione Tag
            // ------------------------------------------------------------------------------------------------
            if (tags.isNotEmpty()) {
                val maxVisibleTags = 3
                val visibleTags = tags.take(maxVisibleTags)
                val remainingCount = tags.size - maxVisibleTags

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    visibleTags.forEach { tagEntity ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f),
                            ),
                        ) {
                            Text(
                                text = "#${tagEntity.tag}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (remainingCount > 0) {
                        Text(
                            text = "+$remainingCount...",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))


            // ------------------------------------------------------------------------------------------------
            // Pie di pagina: Interazioni (Like e Conteggio)
            // ------------------------------------------------------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Flag else Icons.Outlined.Flag,
                        contentDescription = stringResource(R.string.action_favorite),
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = favoriteCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
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
            val author = UserEntity(email = "user1@gmail.com", username = "User Test 1")
            PostItem(
                post = PostEntity(
                    id = "1",
                    authorEmail = "user1@gmail.com",
                    title = "My first checkpoint!",
                    initialMessage = "This is a sample message for the preview.",
                    timestamp = System.currentTimeMillis() - 7200000,
                    mediaUri = "https://example.com/image.jpg",
                    mediaType = MediaType.IMAGE,
                ),
                author = author,
                tags = listOf(
                    PostTagEntity("1", "gaming"),
                    PostTagEntity("1", "coding"),
                ),
                isLiked = true,
                favoriteCount = 12,
            )
        }
    }
}

@Preview(showBackground = true, name = "Post Item Light")
@Composable
fun PostItemLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val author = UserEntity(email = "user1@gmail.com", username = "User Test 1")
            PostItem(
                post = PostEntity(
                    id = "1",
                    authorEmail = "user1@gmail.com",
                    title = "My first checkpoint!",
                    initialMessage = "This is a sample message for the preview.",
                    timestamp = System.currentTimeMillis() - 300000,
                    mediaUri = "https://example.com/image.jpg",
                    mediaType = MediaType.IMAGE,
                ),
                author = author,
                tags = listOf(
                    PostTagEntity("1", "relax"),
                    PostTagEntity("1", "nature"),
                ),
                isLiked = false,
                favoriteCount = 12,
            )
        }
    }
}
