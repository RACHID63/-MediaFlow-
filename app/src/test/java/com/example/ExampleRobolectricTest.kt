package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.AlbumEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistItemEntity
import com.example.data.model.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MediaFlow", appName)
    }

    @Test
    fun `musical album, songs and playlist CRUD operations`() = runBlocking {
        val albumDao = database.albumDao()
        val songDao = database.songDao()
        val playlistDao = database.playlistDao()

        // 1. Créer un Album Musical
        val albumId = albumDao.insertAlbum(
            AlbumEntity(
                title = "Abbey Road",
                artist = "The Beatles",
                genre = "Rock",
                releaseYear = 1969
            )
        )
        val albums = albumDao.getAllAlbums().first()
        assertEquals(1, albums.size)
        assertEquals("Abbey Road", albums[0].title)
        assertEquals("The Beatles", albums[0].artist)

        // 2. Insérer des Chansons rattachées à l'Album
        val songId = songDao.insertSong(
            SongEntity(
                albumId = albumId,
                title = "Come Together",
                artist = "The Beatles",
                trackNumber = 1,
                durationSeconds = 259
            )
        )
        val albumWithSongs = albumDao.getAlbumWithSongs(albumId).first()
        assertNotNull(albumWithSongs)
        assertEquals(1, albumWithSongs?.songs?.size)
        assertEquals("Come Together", albumWithSongs?.songs?.get(0)?.title)

        // 3. Créer une Playlist et séquencer la chanson
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = "Rock Classics"))
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                songId = songId,
                position = 0
            )
        )
        val playlistWithSongs = playlistDao.getPlaylistWithSongs(playlistId).first()
        assertNotNull(playlistWithSongs)
        assertEquals(1, playlistWithSongs?.items?.size)
        assertEquals("Come Together", playlistWithSongs?.items?.get(0)?.song?.title)

        // 4. Test du réordonnancement par drag & drop (reorder)
        val song2Id = songDao.insertSong(
            SongEntity(
                albumId = albumId,
                title = "Something",
                artist = "The Beatles",
                trackNumber = 2,
                durationSeconds = 182
            )
        )
        val currentSongs = songDao.getSongsForAlbum(albumId).first()
        assertEquals(2, currentSongs.size)
        assertEquals("Come Together", currentSongs[0].title)
        assertEquals("Something", currentSongs[1].title)

        // Simuler le déplacement : Something en piste 1, Come Together en piste 2
        val reordered = listOf(
            currentSongs[1].copy(trackNumber = 1),
            currentSongs[0].copy(trackNumber = 2)
        )
        songDao.updateSongs(reordered)

        val updatedSongs = songDao.getSongsForAlbum(albumId).first()
        assertEquals("Something", updatedSongs[0].title)
        assertEquals("Come Together", updatedSongs[1].title)
    }

    @Test
    fun `playback loop modes and timer state calculations`() {
        val loopModes = com.example.audio.PlaybackLoopMode.values()
        assertEquals(3, loopModes.size)
        assertEquals("Une seule fois", com.example.audio.PlaybackLoopMode.ONCE.label)
        assertEquals("Boucle continue", com.example.audio.PlaybackLoopMode.CONTINUOUS.label)
        assertEquals("Boucle avec minuteur", com.example.audio.PlaybackLoopMode.TIMED_CONTINUOUS.label)

        // Test timer state
        val timer = com.example.audio.PlaybackTimerState(
            isActive = true,
            totalSeconds = 1800, // 30 min
            remainingSeconds = 900 // 15 min restants
        )
        assertEquals("15:00", timer.formattedRemaining())
        assertEquals("30 min", timer.formattedTotal())
        assertEquals(0.5f, timer.progressFraction, 0.01f)
    }

    @Test
    fun `image storage helper copies external uri to internal filesDir permanently`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simuler un fichier temporaire externe (comme un fichier renvoyé par Photo Picker)
        val tempSourceFile = java.io.File(context.cacheDir, "temp_picked_image.jpg").apply {
            writeBytes("fake-jpeg-data".toByteArray())
        }
        val sourceUri = android.net.Uri.fromFile(tempSourceFile).toString()

        val persistedUri = com.example.util.ImageStorageHelper.persistImageToInternalStorage(
            context = context,
            sourceUriStr = sourceUri,
            prefix = "test_album"
        )

        assertNotNull(persistedUri)
        org.junit.Assert.assertTrue(persistedUri!!.startsWith("file://"))

        val targetPath = android.net.Uri.parse(persistedUri).path
        assertNotNull(targetPath)
        val targetFile = java.io.File(targetPath!!)
        org.junit.Assert.assertTrue("Le fichier doit exister dans filesDir/covers", targetFile.exists())
        org.junit.Assert.assertTrue(targetFile.absolutePath.contains("covers"))
        assertEquals("fake-jpeg-data", targetFile.readText())

        // Nettoyage
        com.example.util.ImageStorageHelper.deleteInternalImageIfPresent(context, persistedUri)
        org.junit.Assert.assertFalse("Le fichier interne doit être supprimé", targetFile.exists())
    }
}
