package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour les images et pochettes importées via le Photo Picker.
 */
@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_images ORDER BY date_added DESC")
    fun getAllImages(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(image: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<MediaItemEntity>): List<Long>

    @Delete
    suspend fun deleteImage(image: MediaItemEntity)

    @Query("DELETE FROM media_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)
}
