package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant un Album Musical.
 * Comprend l'artiste, le genre, l'année et la pochette d'album (choisie via le Photo Picker).
 */
@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val genre: String = "Général",
    val releaseYear: Int = 2024,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
