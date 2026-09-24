package com.example.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata per la conversazione di chat privata in tempo reale tra due utenti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUserEmail: String?,
    partner: UserEntity,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(partner.username, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            // ------------------------------------------------------------------------------------------------
            // Barra di Input Messaggio
            // ------------------------------------------------------------------------------------------------
            Surface(
                tonalElevation = MaterialTheme.spacing.small,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                    )
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                            }
                        },
                        enabled = text.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.action_send),
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        },
    ) { padding ->
        // ------------------------------------------------------------------------------------------------
        // Elenco Cronologico dei Messaggi
        // ------------------------------------------------------------------------------------------------
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            items(messages) { message ->
                val isMe = message.senderEmail == currentUserEmail
                ChatBubble(message, isMe)
            }
        }
    }
}

/**
 * Bolla per la visualizzazione del singolo messaggio di chat e dell'orario di invio.
 */
@Composable
fun ChatBubble(message: MessageEntity, isMe: Boolean) {
    val timeFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = MaterialTheme.spacing.medium,
                topEnd = MaterialTheme.spacing.medium,
                bottomStart = if (isMe) MaterialTheme.spacing.medium else MaterialTheme.spacing.extraSmall,
                bottomEnd = if (isMe) MaterialTheme.spacing.extraSmall else MaterialTheme.spacing.medium,
            ),
            tonalElevation = MaterialTheme.spacing.extraExtraSmall,
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.smallMedium, vertical = MaterialTheme.spacing.small),
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = timeString,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall, vertical = MaterialTheme.spacing.extraExtraSmall),
        )
    }
}

@Preview(showSystemUi = true, name = "Chat Dark")
@Composable
fun ChatScreenDarkPreview() {
    val partner = UserEntity("partner@gmail.com", "Partner User")
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            ChatScreen(
                currentUserEmail = "me@gmail.com",
                partner = partner,
                messages = listOf(
                    MessageEntity(senderEmail = "me@gmail.com", receiverEmail = partner.email, content = "Hi there!"),
                    MessageEntity(senderEmail = partner.email, receiverEmail = "me@gmail.com", content = "Hello! How are you?"),
                ),
                onBack = {},
                onSendMessage = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Chat Light")
@Composable
fun ChatScreenLightPreview() {
    val partner = UserEntity("partner@gmail.com", "Partner User")
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            ChatScreen(
                currentUserEmail = "me@gmail.com",
                partner = partner,
                messages = listOf(
                    MessageEntity(senderEmail = "me@gmail.com", receiverEmail = partner.email, content = "Hi there!"),
                    MessageEntity(senderEmail = partner.email, receiverEmail = "me@gmail.com", content = "Hello! How are you?"),
                ),
                onBack = {},
                onSendMessage = {},
            )
        }
    }
}
