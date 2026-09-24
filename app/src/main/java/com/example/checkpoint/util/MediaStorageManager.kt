package com.example.checkpoint.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Gestione della persistenza locale dei file multimediali e della codifica/compressione Base64 per Firestore e Room.
 */
object MediaStorageManager {

    private val ALLOWED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

    /**
     * Salva un file multimediale selezionato nella memoria interna dell'applicazione.
     */
    fun saveMediaToInternalStorage(context: Context, uri: Uri): Uri? {
        val extension = getFileExtension(context, uri) ?: "jpg"
        if (!isExtensionAllowed(extension)) return null

        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fileName = "img_${UUID.randomUUID()}.$extension"
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
            MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(uri.path ?: "")).toString())
        }?.lowercase(Locale.ROOT)
    }

    private fun isExtensionAllowed(extension: String?): Boolean {
        if (extension == null) return false
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension)
    }

    /**
     * Comprime un'immagine e la codifica in formato Base64 ridimensionando le dimensioni su un thread I/O.
     */
    suspend fun encodeToOptimizedBase64(
        context: Context,
        uriString: String?,
        isProfilePicture: Boolean = false,
    ): String? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        if (uriString.startsWith("data:") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return@withContext uriString
        }

        try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val filePath = uri.path ?: uriString.removePrefix("file://")
                val file = File(filePath)
                if (file.exists()) FileInputStream(file) else null
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) return@withContext uriString

            val bytes = inputStream.use { it.readBytes() }
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext uriString

            val maxDimension = if (isProfilePicture) 400f else 800f
            val width = originalBitmap.width
            val height = originalBitmap.height

            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = minOf(maxDimension / width, maxDimension / height)
                val newWidth = (width * ratio).roundToInt()
                val newHeight = (height * ratio).roundToInt()
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val compressedBytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }
}
