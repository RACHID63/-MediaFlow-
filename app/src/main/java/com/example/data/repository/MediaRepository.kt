package com.example.data.repository

import com.example.data.dao.AlbumDao
import com.example.data.dao.MediaItemDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.SongDao
import com.example.data.model.AlbumEntity
import com.example.data.model.AlbumWithSongs
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistItemEntity
import com.example.data.model.PlaylistWithSongs
import com.example.data.model.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository gérant l'accès aux albums musicaux, chansons, playlists et images importées via Photo Picker.
 */
class MediaRepository(
    private val albumDao: AlbumDao,
    private val songDao: SongDao,
    private val mediaItemDao: MediaItemDao,
    private val playlistDao: PlaylistDao
) {
    // --- Albums Musicaux ---
    val allAlbums: Flow<List<AlbumEntity>> = albumDao.getAllAlbums()

    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithSongs?> =
        albumDao.getAlbumWithSongs(albumId)

    suspend fun createAlbum(
        title: String,
        artist: String,
        genre: String = "Pop",
        releaseYear: Int = 2024,
        coverUri: String? = null
    ): Long {
        val album = AlbumEntity(
            title = title,
            artist = artist,
            genre = genre,
            releaseYear = releaseYear,
            coverUri = coverUri
        )
        return albumDao.insertAlbum(album)
    }

    suspend fun updateAlbum(album: AlbumEntity) {
        albumDao.updateAlbum(album)
    }

    suspend fun updateAlbumCover(albumId: Long, coverUri: String?) {
        albumDao.updateCover(albumId, coverUri)
    }

    suspend fun deleteAlbum(albumId: Long) {
        albumDao.deleteAlbumById(albumId)
    }

    // --- Chansons (Songs) ---
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()

    fun getSongsForAlbum(albumId: Long): Flow<List<SongEntity>> =
        songDao.getSongsForAlbum(albumId)

    suspend fun addSong(
        albumId: Long,
        title: String,
        artist: String,
        trackNumber: Int = 1,
        durationSeconds: Int = 200,
        audioUri: String? = null
    ): Long {
        val song = SongEntity(
            albumId = albumId,
            title = title,
            artist = artist,
            trackNumber = trackNumber,
            durationSeconds = durationSeconds,
            audioUri = audioUri
        )
        return songDao.insertSong(song)
    }

    suspend fun addSongs(songs: List<SongEntity>): List<Long> =
        songDao.insertSongs(songs)

    suspend fun updateSong(song: SongEntity) {
        songDao.updateSong(song)
    }

    suspend fun reorderAlbumSongs(songs: List<SongEntity>) {
        val updated = songs.mapIndexed { index, song ->
            song.copy(trackNumber = index + 1)
        }
        songDao.updateSongs(updated)
    }

    suspend fun updateSongAudio(songId: Long, audioUri: String) {
        val song = songDao.getSongByIdOnce(songId)
        if (song != null) {
            songDao.updateSong(song.copy(audioUri = audioUri))
        }
    }

    suspend fun deleteSong(songId: Long) {
        songDao.deleteSongById(songId)
    }

    // --- Images / Pochettes (Photo Picker) ---
    val allImages: Flow<List<MediaItemEntity>> = mediaItemDao.getAllImages()

    suspend fun addImages(images: List<MediaItemEntity>): List<Long> =
        mediaItemDao.insertImages(images)

    suspend fun deleteImage(imageId: Long) {
        mediaItemDao.deleteImageById(imageId)
    }

    // --- Playlists de chansons ---
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> =
        playlistDao.getPlaylistWithSongs(playlistId)

    suspend fun createPlaylist(name: String, description: String = "", coverUri: String? = null): Long {
        val playlist = PlaylistEntity(name = name, description = description, coverUri = coverUri)
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int): Long {
        val item = PlaylistItemEntity(
            playlistId = playlistId,
            songId = songId,
            position = position
        )
        return playlistDao.insertPlaylistItem(item)
    }

    suspend fun reorderPlaylist(playlistId: Long, items: List<PlaylistItemEntity>) {
        playlistDao.updatePlaylistItems(items)
    }

    suspend fun removeSongFromPlaylist(playlistItemId: Long) {
        playlistDao.deletePlaylistItem(playlistItemId)
    }
}
