package com.example.checkpoint.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

/**
 * Gestisce l'upload e il salvataggio dei file multimediali.
 * Utilizza Firebase Storage per l'archiviazione remota dei file statici,
 * restituendo l'URL pubblico per evitare il salvataggio di dati pesanti (come stringhe Base64)
 * nei database Firestore o Room, migliorando le prestazioni delle query e il consumo di memoria.
 */
object MediaStorageManager {
    private val ALLOWED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

    /**
     * Copia un'immagine dalla memoria esterna alla memoria privata dell'app.
     * Garantisce l'accesso locale al file prima delle operazioni di upload o ritaglio.
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
     * Esegue l'upload di un'immagine locale su Firebase Storage.
     *
     * @param context Il contesto dell'applicazione.
     * @param uriString L'URI locale dell'immagine.
     * @param folder La directory di destinazione su Firebase Storage.
     * @return L'URL HTTPS per il download pubblico, o null in caso di fallimento.
     */
    suspend fun uploadMediaToFirebase(context: Context, uriString: String?, folder: String = "posts_media"): String? {
        if (uriString.isNullOrBlank()) return null

        // Ignora l'upload se l'URI è già un URL remoto valido.
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

            if (inputStream == null) return null

            // Converte i Task asincroni di Firebase in chiamate sequenziali tramite Coroutines.
            val uploadTask = storageRef.putStream(inputStream)
            uploadTask.await()

            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}