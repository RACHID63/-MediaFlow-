package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant une Playlist musicale.
 * Regroupe et séquence des chansons pour une lecture continue.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
