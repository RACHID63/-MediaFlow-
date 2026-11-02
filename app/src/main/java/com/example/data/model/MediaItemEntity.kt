package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entité représentant une image/pochette importée via le Photo Picker d'Android.
 * Peut être associée à des albums musicaux ou des playlists en tant qu'artwork.
 */
@Entity(
    tableName = "media_images",
    indices = [Index(value = ["uri"], unique = true)]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val title: String,
    val mimeType: String = "image/jpeg",
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long = 0L,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
)
