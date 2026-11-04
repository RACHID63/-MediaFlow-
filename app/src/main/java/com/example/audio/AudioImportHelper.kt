package com.example.audio

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

data class ExtractedAudioMeta(
    val uri: String,
    val title: String,
    val artist: String,
    val albumTitle: String?,
    val durationSeconds: Int,
    val trackNumber: Int = 1,
    val originalFileName: String? = null
)

object AudioImportHelper {

    fun extractMetadata(
        context: Context,
        uri: Uri,
        fallbackArtist: String = "Artiste inconnu",
        trackIndex: Int = 1
    ): ExtractedAudioMeta {
        val contentResolver = context.contentResolver
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationSec = 180

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMsStr?.toLongOrNull()?.let { ms ->
                durationSec = (ms / 1000).toInt().coerceAtLeast(1)
            }
        } catch (_: Exception) {
            // Certains formats audio ou codecs nécessitent une extraction par nom de fichier
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val fileName = queryDisplayName(contentResolver, uri)
        // Si le titre n'était pas dans les tags ID3/RIFF, utiliser le nom du fichier sans son extension (.mp3, .wav, etc.)
        if (title.isNullOrBlank()) {
            title = fileName.substringBeforeLast(".")
        }

        if (artist.isNullOrBlank()) {
            artist = fallbackArtist
        }

        return ExtractedAudioMeta(
            uri = uri.toString(),
            title = title,
            artist = artist,
            albumTitle = album,
            durationSeconds = durationSec,
            trackNumber = trackIndex,
            originalFileName = fileName
        )
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String {
        var name = "Piste_Audio_${System.currentTimeMillis()}"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
