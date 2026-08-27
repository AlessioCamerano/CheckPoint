package com.example.checkpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.util.TimeUtils

@Composable
fun PostItem(
    post: Post,
    onFavoriteClick: () -> Unit = {}
) {
    val timeAgo = TimeUtils.getTimeAgo(post.timestamp)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Autore e Tempo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = post.authorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = timeAgo,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Corpo: Titolo
            Text(
                text = post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // MEDIA SECTION
            if (post.mediaUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                when (post.mediaType) {
                    MediaType.IMAGE -> {
                        AsyncImage(
                            model = post.mediaUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp)),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Messaggio
            Text(
                text = post.initialMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Interazioni (Flag per i preferiti)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (post.isFavorite) Icons.Default.Flag else Icons.Outlined.Flag,
                        contentDescription = "Favorite",
                        tint = if (post.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${post.favoriteCount}",
                    fontSize = 13.sp,
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
                )
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
                    timestamp = System.currentTimeMillis() - 300000 // 5m ago
                )
            )
        }
    }
}
