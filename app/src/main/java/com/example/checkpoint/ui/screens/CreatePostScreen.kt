package com.example.checkpoint.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.components.MediaUploadButton
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.util.MediaStorageManager

@Composable
fun CreatePostScreen(
    state: MainUiState,
    onCreatePost: (String, String, String?, MediaType, Boolean, List<String>) -> Unit = { _, _, _, _, _, _ -> }
) {
    CreatePostScreenContent(state = state, onCreatePost = onCreatePost)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreenContent(
    state: MainUiState,
    onCreatePost: (String, String, String?, MediaType, Boolean, List<String>) -> Unit = { _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("") }
    var currentTag by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var mediaUri by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf(MediaType.NONE) }
    var isMuted by remember { mutableStateOf(false) }

    // Suggerimenti Tag
    val suggestions = remember(currentTag, state.availableTags) {
        if (currentTag.isBlank()) emptyList()
        else state.availableTags.filter { 
            it.lowercase().contains(currentTag.lowercase().removePrefix("#")) && 
            !selectedTags.contains(it) 
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val isVideo = MediaStorageManager.isVideo(context, it)
                val savedUri = MediaStorageManager.saveMediaToInternalStorage(context, it)
                if (savedUri != null) {
                    mediaUri = savedUri.toString()
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                    if (!isVideo) isMuted = false
                }
            }
        }
    )

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

                // Titolo
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

                // Anteprima Media
                if (mediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mediaType == MediaType.IMAGE) {
                            AsyncImage(
                                model = mediaUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Videocam, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Text("Video selected", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .clickable { isMuted = !isMuted }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isMuted) "Muted" else "Audio On",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { mediaUri = null; mediaType = MediaType.NONE },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Messaggio
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

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Tag con Suggerimenti
                var textFieldHeight by remember { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier.onGloballyPositioned { 
                        textFieldHeight = it.size.height 
                    }
                ) {
                    OutlinedTextField(
                        value = currentTag,
                        onValueChange = { currentTag = it },
                        label = { Text("Add Tags (Enter to add)", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp) },
                        placeholder = { Text("#gaming", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val cleanTag = currentTag.trim().removePrefix("#")
                            if (cleanTag.isNotBlank() && !selectedTags.contains(cleanTag)) {
                                selectedTags.add(cleanTag)
                                currentTag = ""
                            }
                        })
                    )
                    
                    if (suggestions.isNotEmpty()) {
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = androidx.compose.ui.unit.IntOffset(0, textFieldHeight)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(top = 4.dp)
                                    .heightIn(max = 200.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0A1118),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                tonalElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    suggestions.forEach { tag ->
                                        Text(
                                            text = "#$tag",
                                            color = Color(0xFF00D4FF),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    selectedTags.add(tag)
                                                    currentTag = ""
                                                }
                                                .padding(16.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista Tag Selezionati
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedTags.forEach { tag ->
                        Surface(
                            color = Color(0xFF0A1118),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF00D4FF).copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selectedTags.remove(tag) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (mediaUri == null) {
                    MediaUploadButton(
                        icon = Icons.Default.AddPhotoAlternate,
                        label = "Add Media",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { 
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tasto Post
            Button(
                onClick = { 
                    if (title.isNotBlank() && initialMessage.isNotBlank()) {
                        onCreatePost(title, initialMessage, mediaUri, mediaType, isMuted, selectedTags.toList())
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
        CreatePostScreenContent(state = MainUiState(isDarkTheme = true))
    }
}

@Preview(showSystemUi = true, name = "Create Post Light")
@Composable
fun CreatePostScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        CreatePostScreenContent(state = MainUiState(isDarkTheme = false))
    }
}
