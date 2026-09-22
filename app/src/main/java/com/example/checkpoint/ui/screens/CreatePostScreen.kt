package com.example.checkpoint.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.components.MediaUploadButton
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.util.MediaEditHelper
import com.example.checkpoint.util.MediaStorageManager
import com.yalantis.ucrop.UCrop

@OptIn(UnstableApi::class)
@Composable
fun CreatePostScreen(
    state: MainUiState,
    onCreatePost: (String, String, String?, MediaType, List<String>) -> Unit = { _, _, _, _, _ -> }
) {
    CreatePostScreenContent(state = state, onCreatePost = onCreatePost)
}

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreenContent(
    state: MainUiState,
    onCreatePost: (String, String, String?, MediaType, List<String>) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("") }
    var currentTag by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var mediaUri by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf(MediaType.NONE) }

    LaunchedEffect(state.postSuccess) {
        if (state.postSuccess) {
            title = ""
            initialMessage = ""
            currentTag = ""
            selectedTags.clear()
            mediaUri = null
            mediaType = MediaType.NONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            title = ""
            initialMessage = ""
            currentTag = ""
            selectedTags.clear()
            mediaUri = null
            mediaType = MediaType.NONE
        }
    }

    // Suggerimenti Tag
    val suggestions = remember(currentTag, state.availableTags) {
        if (currentTag.isBlank()) emptyList()
        else state.availableTags.filter { 
            it.lowercase().contains(currentTag.lowercase().removePrefix("#")) && 
            !selectedTags.contains(it) 
        }
    }

    // Launcher per uCrop
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = result.data?.let { UCrop.getOutput(it) }
                if (resultUri != null) {
                    mediaUri = resultUri.toString()
                    mediaType = MediaType.IMAGE
                }
            }
        }
    )

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val savedUri = MediaStorageManager.saveMediaToInternalStorage(context, it)
                if (savedUri != null) {
                    mediaUri = savedUri.toString()
                    mediaType = MediaType.IMAGE
                }
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.large)
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            
            Text(
                text = stringResource(R.string.post_new_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.post_new_subtitle),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .padding(top = MaterialTheme.spacing.small)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // Titolo
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Messaggio
            OutlinedTextField(
                value = initialMessage,
                onValueChange = { initialMessage = it },
                label = { Text(stringResource(R.string.field_message)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Anteprima Immagine con Tasto Ritaglio
            if (mediaUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Tasto Modifica Immagine
                    IconButton(
                        onClick = { 
                            val intent = MediaEditHelper.startImageCrop(context, Uri.parse(mediaUri)).getIntent(context)
                            cropLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(MaterialTheme.spacing.small)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    
                    // Tasto Chiudi
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
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            } else {
                MediaUploadButton(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = stringResource(R.string.post_add_media),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }

            LaunchedEffect(currentTag) {
                if (currentTag.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            // Campo Tag
            var textFieldHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            Box(
                modifier = Modifier.onGloballyPositioned { 
                    textFieldHeight = with(density) { it.size.height.toDp() }
                }
            ) {
                OutlinedTextField(
                    value = currentTag,
                    onValueChange = { currentTag = it },
                    label = { Text(stringResource(R.string.field_tags_hint)) },
                    placeholder = { Text(stringResource(R.string.field_tags_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
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
                        offset = IntOffset(0, with(density) { textFieldHeight.roundToPx() })
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = MaterialTheme.spacing.extraSmall)
                                .heightIn(max = 200.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            border = BorderStroke(
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

            // Tag Selezionati
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedTags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(
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
                                contentDescription = stringResource(R.string.action_remove),
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

            // Tasto Post
            Button(
                onClick = { 
                    if (title.isNotBlank() && initialMessage.isNotBlank()) {
                        onCreatePost(title, initialMessage, mediaUri, mediaType, selectedTags.toList())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.large)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = title.isNotBlank() && initialMessage.isNotBlank()
            ) {
                Text(stringResource(R.string.post_button), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Preview(showSystemUi = true, name = "Create Post Dark")
@OptIn(UnstableApi::class)
@Composable
fun CreatePostScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        CreatePostScreenContent(state = MainUiState(isDarkTheme = true))
    }
}

@Preview(showSystemUi = true, name = "Create Post Light")
@OptIn(UnstableApi::class)
@Composable
fun CreatePostScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        CreatePostScreenContent(state = MainUiState(isDarkTheme = false))
    }
}
