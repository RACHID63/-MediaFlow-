package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Gestionnaire du stockage persistant et autonome des fichiers audios (MP3, WAV, FLAC, M4A, OGG...)
 * dans le sous-répertoire privé de l'application (context.filesDir/audios/).
 *
 * Résout le problème où la suppression d'un fichier dans le dossier 'Téléchargements'
 * ou un support externe rendrait la musique inaccessible dans l'album.
 */
object AudioStorageHelper {

    private const val AUDIOS_DIRECTORY = "audios"

    /**
     * Copie de manière persistante et autonome un fichier audio importé
     * dans le répertoire privé de l'application (context.filesDir/audios/).
     *
     * @param context Contexte Android
     * @param sourceUriStr URI du fichier audio source (content://, file://, etc.)
     * @param originalFileName Nom original du fichier pour préserver l'extension (.mp3, .wav...)
     * @param prefix Préfixe du nom de fichier généré
     * @return URI permanent au format file:///... ou URI d'origine en cas de fallback
     */
    suspend fun persistAudioToInternalStorage(
        context: Context,
        sourceUriStr: String?,
        originalFileName: String? = null,
        prefix: String = "track"
    ): String? = withContext(Dispatchers.IO) {
        if (sourceUriStr.isNullOrBlank()) return@withContext null

        // 1. Ressource interne de l'application
        if (sourceUriStr.startsWith("android.resource://")) {
            return@withContext sourceUriStr
        }

        // 2. Déjà dans notre dossier privé context.filesDir
        if (sourceUriStr.startsWith("file://")) {
            val localPath = Uri.parse(sourceUriStr).path
            if (localPath != null && localPath.startsWith(context.filesDir.absolutePath)) {
                return@withContext sourceUriStr
            }
        }

        val sourceUri = try {
            Uri.parse(sourceUriStr)
        } catch (_: Exception) {
            return@withContext sourceUriStr
        }

        try {
            val audiosDir = File(context.filesDir, AUDIOS_DIRECTORY).apply {
                if (!exists()) mkdirs()
            }

            val realFileName = originalFileName ?: queryDisplayName(context, sourceUri)
            val extension = determineExtension(context, sourceUri, realFileName)

            val baseName = (realFileName?.substringBeforeLast('.') ?: "track")
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .trim()
                .take(30)
                .ifBlank { "audio" }

            val uniqueFileName = "${prefix}_${System.currentTimeMillis()}_${baseName}$extension"
            val destinationFile = File(audiosDir, uniqueFileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream != null) {
                inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destinationFile).toString()
            } else {
                sourceUriStr
            }
        } catch (e: Exception) {
            Log.e("AudioStorageHelper", "Erreur lors de la copie du fichier audio vers le stockage interne", e)
            sourceUriStr
        }
    }

    private fun determineExtension(context: Context, uri: Uri, fileName: String?): String {
        if (fileName != null && fileName.contains(".")) {
            val ext = fileName.substringAfterLast(".", "").lowercase()
            if (ext in listOf("mp3", "wav", "m4a", "ogg", "flac", "aac", "opus", "wma", "mid", "midi")) {
                return ".$ext"
            }
        }
        val mimeType = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        return when {
            mimeType?.contains("wav", ignoreCase = true) == true -> ".wav"
            mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("m4a", ignoreCase = true) == true -> ".m4a"
            mimeType?.contains("ogg", ignoreCase = true) == true -> ".ogg"
            mimeType?.contains("flac", ignoreCase = true) == true -> ".flac"
            mimeType?.contains("aac", ignoreCase = true) == true -> ".aac"
            else -> ".mp3"
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Supprime le fichier audio privé interne lors de la suppression d'une chanson ou d'un album.
     */
    fun deleteInternalAudioIfPresent(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            if (uriString.startsWith("file://")) {
                val path = Uri.parse(uriString).path ?: return
                val file = File(path)
                if (file.absolutePath.startsWith(context.filesDir.absolutePath) && file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
