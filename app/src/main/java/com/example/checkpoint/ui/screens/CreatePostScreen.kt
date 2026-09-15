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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.components.MediaUploadButton
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
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
                .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(com.example.checkpoint.R.string.post_new_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(com.example.checkpoint.R.string.post_new_subtitle),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                // Titolo
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(com.example.checkpoint.R.string.field_title), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge) },
                    placeholder = { Text(stringResource(com.example.checkpoint.R.string.field_title), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

                // Anteprima Media
                if (mediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.large)
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
                                Text(stringResource(com.example.checkpoint.R.string.post_video_selected), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .clickable { isMuted = !isMuted }
                                        .padding(horizontal = MaterialTheme.spacing.smallMedium, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                                    Text(
                                        text = if (isMuted) stringResource(com.example.checkpoint.R.string.post_media_muted) else stringResource(com.example.checkpoint.R.string.post_media_unmuted),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { mediaUri = null; mediaType = MediaType.NONE },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(MaterialTheme.spacing.small)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                }

                // Messaggio
                OutlinedTextField(
                    value = initialMessage,
                    onValueChange = { initialMessage = it },
                    label = { Text(stringResource(com.example.checkpoint.R.string.field_message), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge) },
                    placeholder = { Text(stringResource(com.example.checkpoint.R.string.comment_placeholder), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

                // Campo Tag con Suggerimenti
                var textFieldHeight by remember { mutableStateOf(0.dp) }
                val density = androidx.compose.ui.platform.LocalDensity.current
                Box(
                    modifier = Modifier.onGloballyPositioned { 
                        textFieldHeight = with(density) { it.size.height.toDp() }
                    }
                ) {
                    OutlinedTextField(
                        value = currentTag,
                        onValueChange = { currentTag = it },
                        label = { Text(stringResource(com.example.checkpoint.R.string.field_tags_hint), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge) },
                        placeholder = { Text(stringResource(com.example.checkpoint.R.string.field_tags_placeholder), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
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
                            offset = androidx.compose.ui.unit.IntOffset(0, with(density) { textFieldHeight.roundToPx() })
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(top = MaterialTheme.spacing.extraSmall)
                                    .heightIn(max = 200.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, 
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                                ),
                                tonalElevation = MaterialTheme.spacing.small
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    suggestions.forEach { tag ->
                                        Text(
                                            text = "#$tag",
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    selectedTags.add(tag)
                                                    currentTag = ""
                                                }
                                                .padding(MaterialTheme.spacing.medium),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

                // Lista Tag Selezionati
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedTags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, 
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small / 2 + 2.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(com.example.checkpoint.R.string.action_remove),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selectedTags.remove(tag) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                if (mediaUri == null) {
                    MediaUploadButton(
                        icon = Icons.Default.AddPhotoAlternate,
                        label = stringResource(com.example.checkpoint.R.string.post_add_media),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { 
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

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
                shape = MaterialTheme.shapes.extraLarge,
                enabled = title.isNotBlank() && initialMessage.isNotBlank()
            ) {
                Text(stringResource(com.example.checkpoint.R.string.post_button), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
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
