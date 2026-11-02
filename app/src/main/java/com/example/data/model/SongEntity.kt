package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entité représentant une chanson appartenant à un album musical.
 */
@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["album_id"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int = 1,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 210,
    @ColumnInfo(name = "audio_uri")
    val audioUri: String? = null
) {
    fun formattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
