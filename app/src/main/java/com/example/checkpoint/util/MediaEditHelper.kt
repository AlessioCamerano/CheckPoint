package com.example.checkpoint.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.common.util.UnstableApi
import com.yalantis.ucrop.UCrop
import java.io.File

object MediaEditHelper {
    /**
     * Avvia uCrop per ritagliare un'immagine.
     */
    fun startImageCrop(context: Context, sourceUri: Uri): UCrop {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))
        
        return UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .withOptions(UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setHideBottomControls(false)
                setShowCropGrid(true)
                setCompressionQuality(90)
            })
    }

    /**
     * Esegue il ritaglio temporale e/o la rimozione audio di un video.
     */
    @UnstableApi
    fun transformVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long = 0,
        endMs: Long = -1,
        removeAudio: Boolean = false,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val destinationFile = File(context.cacheDir, "edited_video_${System.currentTimeMillis()}.mp4")
        val destinationUri = Uri.fromFile(destinationFile)

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(sourceUri)
        
        // Clipping
        if (endMs > startMs) {
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }

        val editedMediaItem = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setRemoveAudio(removeAudio)
            .build()

        transformer.addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                onSuccess(destinationUri)
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                onError(exportException)
            }
        })

        transformer.start(editedMediaItem, destinationFile.absolutePath)
    }
}
