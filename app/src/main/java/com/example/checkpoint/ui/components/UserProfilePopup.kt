package com.example.checkpoint.ui.components

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.theme.StatusOnline
import com.example.checkpoint.ui.theme.spacing

/**
 * Popup per la visualizzazione del profilo utente.
 */
@Composable
fun UserProfilePopup(
    user: UserEntity,
    isFollowing: Boolean = false,
    followersCount: Int = 0,
    followingCount: Int = 0,
    onDismiss: () -> Unit,
    onFollowClick: () -> Unit = {},
    onMessageClick: (UserEntity) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = MaterialTheme.spacing.extraExtraSmall
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.spacing.large)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Immagine del profilo e stato
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.profilePictureUri.isNullOrBlank()) {
                            val avatarModel: Any? = remember(user.profilePictureUri) {
                                val uri = user.profilePictureUri
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
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                // Nome utente
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Indirizzo email
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

                // Statistiche: Follower e Seguiti
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$followingCount",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(com.example.checkpoint.R.string.tab_following),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$followersCount",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(com.example.checkpoint.R.string.tab_followers),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                // Biografia
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
                    )
                } else {
                    Text(
                        text = stringResource(com.example.checkpoint.R.string.profile_no_bio),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                // Azioni profilo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    val buttonColor = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    Button(
                        onClick = onFollowClick,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = if (isFollowing) stringResource(com.example.checkpoint.R.string.action_unfollow) else stringResource(com.example.checkpoint.R.string.action_follow), 
                            maxLines = 1, 
                            softWrap = false
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            onDismiss()
                            onMessageClick(user) 
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(text = stringResource(com.example.checkpoint.R.string.action_message), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
