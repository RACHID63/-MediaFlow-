package com.example.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageHelper {

    private const val COVERS_DIRECTORY = "covers"

    /**
     * Sauvegarde de manière persistante une image (sélectionnée via Photo Picker ou autre URI Android)
     * dans le stockage interne privé de l'application (context.filesDir/covers/).
     *
     * Permet d'éviter la perte des permissions de lecture temporaires accordées par le Photo Picker
     * lors du redémarrage de l'application ou du processus.
     *
     * @param context Contexte Android
     * @param sourceUriStr Chaîne URI de l'image (content://, file://, etc.)
     * @param prefix Préfixe du nom de fichier généré
     * @return URI permanent au format file:///... ou URI d'origine si déjà interne / ressource
     */
    suspend fun persistImageToInternalStorage(
        context: Context,
        sourceUriStr: String?,
        prefix: String = "cover"
    ): String? = withContext(Dispatchers.IO) {
        if (sourceUriStr.isNullOrBlank()) return@withContext null

        // 1. Si c'est une ressource interne de l'application (ex: drawable), pas besoin de copier
        if (sourceUriStr.startsWith("android.resource://")) {
            return@withContext sourceUriStr
        }

        // 2. Si c'est déjà un fichier présent dans notre stockage interne, le conserver
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

        // 3. Copier les octets depuis le ContentResolver vers notre dossier privé
        try {
            val coversDir = File(context.filesDir, COVERS_DIRECTORY).apply {
                if (!exists()) mkdirs()
            }

            val mimeType = try {
                context.contentResolver.getType(sourceUri)
            } catch (_: Exception) {
                null
            }

            val extension = when {
                mimeType?.contains("png", ignoreCase = true) == true -> ".png"
                mimeType?.contains("webp", ignoreCase = true) == true -> ".webp"
                mimeType?.contains("gif", ignoreCase = true) == true -> ".gif"
                else -> ".jpg"
            }

            val uniqueFileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$extension"
            val destinationFile = File(coversDir, uniqueFileName)

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
            e.printStackTrace()
            // En cas d'exception inattendue, conserver le URI d'origine comme solution de repli
            sourceUriStr
        }
    }

    /**
     * Supprime un ancien fichier d'image du stockage interne s'il s'agissait d'un fichier créé par l'app.
     */
    fun deleteInternalImageIfPresent(context: Context, uriString: String?) {
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
