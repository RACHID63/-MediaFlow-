package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AlbumEntity
import com.example.data.model.AlbumWithSongs
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion des albums musicaux.
 */
@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbumById(id: Long): Flow<AlbumEntity?>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithSongs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteAlbumById(id: Long)

    @Query("UPDATE albums SET coverUri = :coverUri WHERE id = :albumId")
    suspend fun updateCover(albumId: Long, coverUri: String?)
}
