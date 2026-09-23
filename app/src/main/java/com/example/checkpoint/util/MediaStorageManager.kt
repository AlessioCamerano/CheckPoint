package com.example.checkpoint.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

/**
 * Gestisce il salvataggio e l'elaborazione delle immagini dell'app.
 */
object MediaStorageManager {

    private val ALLOWED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

    /**
     * Copia un'immagine alla memoria privata dell'app se l'estensione è valida.
     */
    fun saveMediaToInternalStorage(context: Context, uri: Uri): Uri? {
        val extension = getFileExtension(context, uri)

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
            MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(uri.path!!)).toString())
        }?.lowercase(Locale.ROOT)
    }

    private fun isExtensionAllowed(extension: String?): Boolean {
        if (extension == null) return false
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension)
    }

    /**
     * Converte un'immagine locale in una stringa Base64 data URI compressa.
     */
    fun convertUriToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("data:") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val filePath = uri.path ?: uriString.removePrefix("file://")
                val file = File(filePath)
                if (file.exists()) FileInputStream(file) else null
            } else {
                context.contentResolver.openInputStream(uri)
            }
            if (inputStream == null) return uriString

            val bytes = inputStream.use { it.readBytes() }
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return uriString

            val maxDimension = 600
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = Math.min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
                val newWidth = Math.round(width * ratio)
                val newHeight = Math.round(height * ratio)
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
            val compressedBytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }

    /**
     * Carica un'immagine su Firebase Storage e restituisce l'URL HTTPS pubblico.
     * In caso di errore/piano non attivo, esegue il fallback automatico su Base64.
     */
    suspend fun uploadMediaToFirebase(context: Context, uriString: String?, folder: String = "posts_media"): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            val extension = getFileExtension(context, uri) ?: "jpg"
            val fileName = "${UUID.randomUUID()}.$extension"
            val storageRef = FirebaseStorage.getInstance().reference.child("$folder/$fileName")

            val inputStream: InputStream? = if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val filePath = uri.path ?: uriString.removePrefix("file://")
                val file = File(filePath)
                if (file.exists()) FileInputStream(file) else null
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) return uriString

            val uploadTask = storageRef.putStream(inputStream)
            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            convertUriToBase64(context, uriString)
        }
    }
}
