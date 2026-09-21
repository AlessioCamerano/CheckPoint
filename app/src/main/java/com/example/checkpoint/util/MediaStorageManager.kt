package com.example.checkpoint.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

/**
 * Gestisce il salvataggio dei media nella memoria interna dell'app.
 */
object MediaStorageManager {

    private val ALLOWED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
    private val ALLOWED_VIDEO_EXTENSIONS = listOf("mp4", "webm")

    /**
     * Copia un file multimediale alla memoria privata dell'app se l'estensione è valida.
     */
    fun saveMediaToInternalStorage(context: Context, uri: Uri): Uri? {
        val extension = getFileExtension(context, uri)

        if (!isExtensionAllowed(extension)) return null

        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val prefix = if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) "img" else "vid"
            val fileName = "${prefix}_${UUID.randomUUID()}.$extension"
            val file = File(context.filesDir, fileName)
            
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
        } else {
            MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(uri.path!!)).toString())
        }?.lowercase(Locale.ROOT)
    }

    private fun isExtensionAllowed(extension: String?): Boolean {
        if (extension == null) return false
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension) || ALLOWED_VIDEO_EXTENSIONS.contains(extension)
    }
    
    fun isVideo(context: Context, uri: Uri): Boolean {
        val extension = getFileExtension(context, uri)
        return ALLOWED_VIDEO_EXTENSIONS.contains(extension)
    }

    fun getVideoDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
