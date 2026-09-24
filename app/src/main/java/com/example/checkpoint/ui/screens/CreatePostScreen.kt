package com.example.checkpoint.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.ui.components.MediaUploadButton
import com.example.checkpoint.ui.state.FeedUiState
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.util.MediaEditHelper
import com.example.checkpoint.util.MediaStorageManager
import com.yalantis.ucrop.UCrop

/**
 * Schermata per la creazione e pubblicazione di un nuovo Checkpoint/Post.
 */
@Composable
fun CreatePostScreen(
    state: FeedUiState,
    onCreatePost: (String, String, String?, MediaType, List<String>) -> Unit = { _, _, _, _, _ -> },
) {
    CreatePostScreenContent(state = state, onCreatePost = onCreatePost)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreenContent(
    state: FeedUiState,
    onCreatePost: (String, String, String?, MediaType, List<String>) -> Unit = { _, _, _, _, _ -> },
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
        },
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
        },
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.large)
                .verticalScroll(scrollState)
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // ------------------------------------------------------------------------------------------------
            // Titolo e Sottotitolo
            // ------------------------------------------------------------------------------------------------
            Text(
                text = stringResource(R.string.post_new_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.post_new_subtitle),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .padding(top = MaterialTheme.spacing.small)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))


            // ------------------------------------------------------------------------------------------------
            // Campi di Testo: Titolo e Messaggio
            // ------------------------------------------------------------------------------------------------
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            OutlinedTextField(
                value = initialMessage,
                onValueChange = { initialMessage = it },
                label = { Text(stringResource(R.string.field_message)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = MaterialTheme.shapes.extraLarge,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))


            // ------------------------------------------------------------------------------------------------
            // Sezione Immagine/Media
            // ------------------------------------------------------------------------------------------------
            if (mediaUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    IconButton(
                        onClick = {
                            val intent = MediaEditHelper.startImageCrop(context, Uri.parse(mediaUri)).getIntent(context)
                            cropLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(MaterialTheme.spacing.small)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { mediaUri = null; mediaType = MediaType.NONE },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MaterialTheme.spacing.small)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }


            // ------------------------------------------------------------------------------------------------
            // Gestione Tag
            // ------------------------------------------------------------------------------------------------
            LaunchedEffect(currentTag) {
                if (currentTag.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            var textFieldHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier.onGloballyPositioned {
                    textFieldHeight = with(density) { it.size.height.toDp() }
                },
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
                    }),
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                selectedTags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "#$tag",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small / 2 + 2.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_remove),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTags.remove(tag) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))


            // ------------------------------------------------------------------------------------------------
            // Pulsante di Invio
            // ------------------------------------------------------------------------------------------------
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
                enabled = title.isNotBlank() && initialMessage.isNotBlank(),
            ) {
                Text(
                    text = stringResource(R.string.post_button),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Preview(showSystemUi = true, name = "Create Post Dark")
@Composable
fun CreatePostScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        CreatePostScreenContent(state = FeedUiState())
    }
}

@Preview(showSystemUi = true, name = "Create Post Light")
@Composable
fun CreatePostScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        CreatePostScreenContent(state = FeedUiState())
    }
}
