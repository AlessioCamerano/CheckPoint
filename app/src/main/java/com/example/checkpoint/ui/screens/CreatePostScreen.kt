package com.example.checkpoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkpoint.ui.components.MediaUploadButton
import com.example.checkpoint.ui.theme.CheckPointTheme

@Composable
fun CreatePostScreen(
    onCreatePost: (String, String) -> Unit = { _, _ -> }
) {
    CreatePostScreenContent(onCreatePost = onCreatePost)
}

@Composable
fun CreatePostScreenContent(
    onCreatePost: (String, String) -> Unit = { _, _ -> }
) {
    var title by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Checkpoint",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Share your thoughts with the community",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campo per il Titolo
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp) },
                    placeholder = { Text("What's on your mind?", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo per il Messaggio
                OutlinedTextField(
                    value = initialMessage,
                    onValueChange = { initialMessage = it },
                    label = { Text("Initial chat message", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp) },
                    placeholder = { Text("Start the conversation...", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Do you want to show something in the post?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaUploadButton(
                        icon = Icons.Default.Image,
                        label = "Image",
                        modifier = Modifier.weight(1f),
                        onClick = { /* Caricamento immagine */ }
                    )
                    MediaUploadButton(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        modifier = Modifier.weight(1f),
                        onClick = { /* Caricamento video */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tasto Post
            Button(
                onClick = { 
                    if (title.isNotBlank() && initialMessage.isNotBlank()) {
                        onCreatePost(title, initialMessage)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                enabled = title.isNotBlank() && initialMessage.isNotBlank()
            ) {
                Text("Post", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Create Post Dark")
@Composable
fun CreatePostScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        CreatePostScreenContent()
    }
}

@Preview(showSystemUi = true, name = "Create Post Light")
@Composable
fun CreatePostScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        CreatePostScreenContent()
    }
}
