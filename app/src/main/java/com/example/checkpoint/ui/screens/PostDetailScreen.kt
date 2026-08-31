package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.components.PostItem
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.util.TimeUtils

/**
 * Schermata di dettaglio di un Checkpoint con discussione
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postWithAuthor: PostWithAuthor,
    comments: List<CommentEntity>,
    onBack: () -> Unit,
    onAddComment: (String, String?) -> Unit,
    onToggleLike: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) }
    // Stato per gestire i commenti espansi/collassati
    val expandedComments = remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Area di inserimento commenti/risposte
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Indicatore visivo se si sta rispondendo a un commento specifico
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Replying to ${replyingTo?.authorName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { replyingTo = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(commentText, replyingTo?.id)
                                    commentText = ""
                                    replyingTo = null
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // Logica per ricostruire l'albero dei commenti e filtrare quelli visibili (espansi)
        val visibleComments = remember(comments, expandedComments.value) {
            val roots = comments.filter { it.parentCommentId == null }
            val map = comments.groupBy { it.parentCommentId }
            
            val result = mutableListOf<Pair<CommentEntity, Int>>()
            fun addRecursive(parent: CommentEntity, depth: Int) {
                result.add(parent to depth)
                if (expandedComments.value.contains(parent.id)) {
                    map[parent.id]?.forEach { child ->
                        addRecursive(child, depth + 1)
                    }
                }
            }
            roots.forEach { addRecursive(it, 0) }
            result
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header: Post originale
            item {
                PostItem(
                    post = postWithAuthor.post,
                    author = postWithAuthor.author,
                    tags = postWithAuthor.tags,
                    isLiked = postWithAuthor.isLikedByCurrentUser,
                    onFavoriteClick = { onToggleLike(postWithAuthor.post.id) }
                )
            }
            
            item {
                Text(
                    text = "Comments (${comments.size})",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Lista dei commenti nidificati
            items(visibleComments) { (comment, depth) ->
                val hasReplies = comments.any { it.parentCommentId == comment.id }
                val isExpanded = expandedComments.value.contains(comment.id)
                
                CommentItem(
                    comment = comment,
                    depth = depth,
                    isExpanded = isExpanded,
                    hasReplies = hasReplies,
                    onReplyClick = { replyingTo = it },
                    onToggleExpand = {
                        val current = expandedComments.value
                        expandedComments.value = if (isExpanded) current - comment.id else current + comment.id
                    }
                )
            }
        }
    }
}

/**
 * Singolo commento.
 */
@Composable
fun CommentItem(
    comment: CommentEntity,
    depth: Int = 0,
    isExpanded: Boolean = false,
    hasReplies: Boolean = false,
    onReplyClick: (CommentEntity) -> Unit,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clickable(enabled = hasReplies) { onToggleExpand() }
    ) {
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasReplies) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${TimeUtils.getTimeAgo(comment.timestamp)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { onReplyClick(comment) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Post Detail Dark")
@Composable
fun PostDetailScreenDarkPreview() {
    val author = UserEntity("author@gmail.com", "Author", "pass")
    val post = Post(authorName = "Author", authorEmail = "author@gmail.com", title = "Discussion Post", initialMessage = "What do you think?")
    val comments = listOf(
        CommentEntity(id = "1", postId = post.id, authorEmail = "u1@g.com", authorName = "User 1", content = "Great post!"),
        CommentEntity(id = "2", postId = post.id, authorEmail = "u2@g.com", authorName = "User 2", content = "I agree.", parentCommentId = "1")
    )
    
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PostDetailScreen(
                postWithAuthor = PostWithAuthor(post, author),
                comments = comments,
                onBack = {},
                onAddComment = { _, _ -> },
                onToggleLike = {}
            )
        }
    }
}
