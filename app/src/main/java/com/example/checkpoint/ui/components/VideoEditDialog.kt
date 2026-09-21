package com.example.checkpoint.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.checkpoint.R
import com.example.checkpoint.util.MediaEditHelper
import com.example.checkpoint.util.MediaStorageManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Composable
fun VideoEditDialog(
    videoUri: Uri,
    onDismiss: () -> Unit,
    onVideoEdited: (Uri) -> Unit
) {
    val context = LocalContext.current
    val duration = remember(videoUri) { MediaStorageManager.getVideoDuration(context, videoUri) }
    
    var range by remember { mutableStateOf(0f..duration.toFloat()) }
    var previousRange by remember { mutableStateOf(range) }
    var removeAudio by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    // rilascio del player
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Video Preview
    LaunchedEffect(range) {
        if (range.start != previousRange.start) {
            exoPlayer.pause()
            isPlaying = false
            exoPlayer.seekTo(range.start.toLong())
        } else if (range.endInclusive != previousRange.endInclusive) {
            exoPlayer.pause()
            isPlaying = false
            exoPlayer.seekTo(range.endInclusive.toLong())
        }
        previousRange = range
    }

    // Gestione Loop del video nel range selezionato
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                if (exoPlayer.currentPosition < range.start.toLong() || exoPlayer.currentPosition > range.endInclusive.toLong()) {
                    exoPlayer.seekTo(range.start.toLong())
                }
                exoPlayer.play()
                delay(100.milliseconds)
            }
        } else {
            exoPlayer.pause()
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.video_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Video Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = {
                            PlayerView(it).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Tasto Play/Pause overlay
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.video_edit_trim),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(range.start / 1000).toInt()}s", style = MaterialTheme.typography.labelSmall)
                    Text("${(range.endInclusive / 1000).toInt()}s", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mute Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = removeAudio,
                        onCheckedChange = { removeAudio = it }
                    )
                    Text(stringResource(R.string.video_edit_mute))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isProcessing) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.video_edit_processing), style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = {
                                isProcessing = true
                                isPlaying = false
                                MediaEditHelper.transformVideo(
                                    context = context,
                                    sourceUri = videoUri,
                                    startMs = range.start.toLong(),
                                    endMs = range.endInclusive.toLong(),
                                    removeAudio = removeAudio,
                                    onSuccess = { editedUri ->
                                        isProcessing = false
                                        onVideoEdited(editedUri)
                                    },
                                    onError = {
                                        isProcessing = false
                                        // Gestione errore
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(R.string.video_edit_save))
                        }
                    }
                }
            }
        }
    }
}
