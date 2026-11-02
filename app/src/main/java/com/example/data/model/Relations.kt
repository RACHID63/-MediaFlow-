package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation un-à-plusieurs : un Album musical contenant toutes ses chansons.
 */
data class AlbumWithSongs(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val songs: List<SongEntity>
)

/**
 * Entrée de playlist combinant la position et les données de la chanson.
 */
data class PlaylistItemWithSong(
    @Embedded
    val playlistItem: PlaylistItemEntity,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "id"
    )
    val song: SongEntity
)

/**
 * Relation complète : une Playlist avec l'ensemble de ses chansons ordonnées pour la lecture continue.
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        entity = PlaylistItemEntity::class,
        parentColumn = "id",
        entityColumn = "playlist_id"
    )
    val items: List<PlaylistItemWithSong>
)
