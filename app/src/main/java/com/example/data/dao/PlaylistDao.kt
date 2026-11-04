package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistItemEntity
import com.example.data.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion des playlists de chansons et leur séquençage.
 */
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: Long): Int

    @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistItemsOnce(playlistId: Long): List<PlaylistItemEntity>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Update
    suspend fun updatePlaylistItems(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlist_songs WHERE id = :id")
    suspend fun deletePlaylistItem(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    @Transaction
    suspend fun replacePlaylistItems(playlistId: Long, items: List<PlaylistItemEntity>) {
        clearPlaylistItems(playlistId)
        insertPlaylistItems(items)
    }
}
