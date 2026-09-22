package com.example.checkpoint.util

import android.content.Context
import android.net.Uri
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
}
