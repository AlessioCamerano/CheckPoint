package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.theme.CheckPointTheme

/**
 * Schermata che raccoglie le Chat Dirette e i Checkpoint salvati.
 */
@Composable
fun MessagesScreen(
    uiState: MainUiState,
    onPartnerClick: (UserEntity) -> Unit = {},
    onPostClick: (PostWithAuthor) -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onUserClick: (UserEntity) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Directs", "Saved Checkpoints")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Messages & Saves",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tab per passare tra Messaggi e Salvati
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

        if (selectedTab == 0) {
            // Sezione Messaggi Diretti
            if (uiState.chatPartners.isEmpty()) {
                MessagesEmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    text = "No messages yet."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.chatPartners) { partner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            UserItem(user = partner, onClick = onPartnerClick)
                            // Mostra il pallino se ci sono messaggi non letti
                            if (uiState.chatPartnersUnread.contains(partner.email)) {
                                UnreadDot(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Sezione Checkpoint Salvati
            val savedPosts = uiState.posts.filter { it.isLikedByCurrentUser }
            if (savedPosts.isEmpty()) {
                MessagesEmptyState(
                    icon = Icons.Default.Flag,
                    text = "No saved checkpoints."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

/**
 * Rappresentazione compatta di un post per la lista dei salvati.
 */
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Avatar del post author
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { onUserClick(postWithAuthor.author) },
                contentAlignment = Alignment.Center
            ) {
                if (postWithAuthor.author.profilePictureUri != null) {
                    AsyncImage(
                        model = postWithAuthor.author.profilePictureUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = postWithAuthor.post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Checkpoint by ${postWithAuthor.author.username}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (postWithAuthor.hasUnreadComments) {
                    UnreadDot()
                    Spacer(modifier = Modifier.width(12.dp))
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
}

@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun MessagesEmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
        }
    }
}
