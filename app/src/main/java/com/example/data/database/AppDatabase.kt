package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlbumDao
import com.example.data.dao.MediaItemDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.SongDao
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistItemEntity
import com.example.data.model.SongEntity

/**
 * Base de données Room principale pour l'application musicale et multimédia.
 */
@Database(
    entities = [
        AlbumEntity::class,
        SongEntity::class,
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun songDao(): SongDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_flow.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
