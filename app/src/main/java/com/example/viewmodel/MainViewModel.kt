package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.AudioImportHelper
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackLoopMode
import com.example.audio.PlaybackTimerState
import com.example.data.model.AlbumEntity
import com.example.data.model.AlbumWithSongs
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistItemEntity
import com.example.data.model.PlaylistWithSongs
import com.example.data.model.SongEntity
import com.example.data.repository.MediaRepository
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Albums : AppScreen
    object Playlists : AppScreen
    object CoversGallery : AppScreen
    data class AlbumDetail(val albumId: Long) : AppScreen
    data class PlaylistDetail(val playlistId: Long) : AppScreen
    object NowPlaying : AppScreen
}

class MainViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    private val audioPlayerManager = AudioPlayerManager(application.applicationContext)

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Albums)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<AppScreen>()

    val allAlbums: StateFlow<List<AlbumEntity>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allImages: StateFlow<List<MediaItemEntity>> = repository.allImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Lecteur Musical & État de lecture continue ---
    private val _currentPlayingSong = MutableStateFlow<SongEntity?>(null)
    val currentPlayingSong: StateFlow<SongEntity?> = _currentPlayingSong.asStateFlow()

    private val _currentCoverUri = MutableStateFlow<String?>(null)
    val currentCoverUri: StateFlow<String?> = _currentCoverUri.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgressSec = MutableStateFlow(0)
    val playbackProgressSec: StateFlow<Int> = _playbackProgressSec.asStateFlow()

    // Mode de lecture : une seule fois, boucle continue, boucle avec timeline (minuteur)
    private val _loopMode = MutableStateFlow(PlaybackLoopMode.CONTINUOUS)
    val loopMode: StateFlow<PlaybackLoopMode> = _loopMode.asStateFlow()

    // Minuteur / Timeline de sommeil
    private val _timerState = MutableStateFlow(PlaybackTimerState())
    val timerState: StateFlow<PlaybackTimerState> = _timerState.asStateFlow()

    // Message d'information (ex: "Minuteur expiré")
    private val _playbackMessage = MutableStateFlow<String?>(null)
    val playbackMessage: StateFlow<String?> = _playbackMessage.asStateFlow()

    private var currentQueue: List<SongEntity> = emptyList()
    private var currentQueueIndex: Int = 0
    private var tickerJob: Job? = null

    init {
        audioPlayerManager.onCompletionListener = {
            viewModelScope.launch {
                onTrackAutoCompleted()
            }
        }
        startPlaybackTicker()
        migrateLegacyContentUris()
    }

    private fun migrateLegacyContentUris() {
        viewModelScope.launch {
            try {
                val albums = repository.allAlbums.first()
                for (album in albums) {
                    val cover = album.coverUri
                    if (cover != null && cover.startsWith("content://")) {
                        val persisted = ImageStorageHelper.persistImageToInternalStorage(
                            context = getApplication(),
                            sourceUriStr = cover,
                            prefix = "album_${album.id}"
                        )
                        if (persisted != null && persisted != cover) {
                            repository.updateAlbumCover(album.id, persisted)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val song = _currentPlayingSong.value
                if (_isPlaying.value && song != null) {
                    // 1. Décompte de la timeline / minuteur si actif
                    if (_timerState.value.isActive) {
                        val currentRemaining = _timerState.value.remainingSeconds
                        if (currentRemaining <= 1) {
                            // Temps écoulé : arrêt programmé de la lecture
                            _timerState.value = PlaybackTimerState()
                            _isPlaying.value = false
                            audioPlayerManager.pause()
                            _playbackMessage.value = "Minuteur écoulé : lecture terminée"
                        } else {
                            _timerState.value = _timerState.value.copy(remainingSeconds = currentRemaining - 1)
                        }
                    }

                    // 2. Progression de la lecture audio
                    if (!song.audioUri.isNullOrBlank() && audioPlayerManager.isPlaying()) {
                        _playbackProgressSec.value = audioPlayerManager.getCurrentPositionSeconds()
                    } else {
                        // Ticker simulé si fichier d'exemple ou lecture sans flux audio direct
                        val nextSec = _playbackProgressSec.value + 1
                        if (nextSec >= song.durationSeconds) {
                            onTrackAutoCompleted()
                        } else {
                            _playbackProgressSec.value = nextSec
                        }
                    }
                }
            }
        }
    }

    /**
     * Gestion de la fin automatique d'un morceau selon le mode choisi :
     * - ONCE : S'arrête à la fin du dernier morceau de l'album / playlist
     * - CONTINUOUS : Repart en boucle continue à l'infini
     * - TIMED_CONTINUOUS : Continue en boucle tant que la timeline n'est pas écoulée
     */
    private fun onTrackAutoCompleted() {
        if (currentQueue.isEmpty()) return

        // Vérifier si le minuteur est arrivé à terme
        if (_timerState.value.isActive && _timerState.value.remainingSeconds <= 0) {
            _isPlaying.value = false
            audioPlayerManager.pause()
            _timerState.value = PlaybackTimerState()
            _playbackMessage.value = "Minuteur écoulé : lecture terminée"
            return
        }

        when (_loopMode.value) {
            PlaybackLoopMode.ONCE -> {
                if (currentQueueIndex + 1 < currentQueue.size) {
                    currentQueueIndex += 1
                    val nextSong = currentQueue[currentQueueIndex]
                    playSong(nextSong, _currentCoverUri.value, currentQueue)
                    resolveCoverForSong(nextSong)
                } else {
                    // Fin de l'album ou de la playlist atteinte en mode 1 fois
                    _isPlaying.value = false
                    audioPlayerManager.stop()
                    _playbackProgressSec.value = 0
                    _playbackMessage.value = "Fin de l'album / playlist (lecture terminée)"
                }
            }
            PlaybackLoopMode.CONTINUOUS, PlaybackLoopMode.TIMED_CONTINUOUS -> {
                currentQueueIndex = (currentQueueIndex + 1) % currentQueue.size
                val nextSong = currentQueue[currentQueueIndex]
                playSong(nextSong, _currentCoverUri.value, currentQueue)
                resolveCoverForSong(nextSong)
            }
        }
    }

    fun setLoopMode(mode: PlaybackLoopMode) {
        _loopMode.value = mode
        if (mode != PlaybackLoopMode.TIMED_CONTINUOUS && _timerState.value.isActive) {
            // Si l'utilisateur passe explicitement à ONCE ou CONTINUOUS sans minuteur
            _timerState.value = PlaybackTimerState()
        } else if (mode == PlaybackLoopMode.TIMED_CONTINUOUS && !_timerState.value.isActive) {
            // Par défaut si sélectionné directement, régler sur 30 minutes
            startSleepTimer(30)
        }
    }

    fun startSleepTimer(durationMinutes: Int) {
        val totalSec = durationMinutes * 60
        _timerState.value = PlaybackTimerState(
            isActive = true,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        _loopMode.value = PlaybackLoopMode.TIMED_CONTINUOUS
        _playbackMessage.value = "Minuteur activé : $durationMinutes minutes"
    }

    fun cancelSleepTimer() {
        _timerState.value = PlaybackTimerState()
        if (_loopMode.value == PlaybackLoopMode.TIMED_CONTINUOUS) {
            _loopMode.value = PlaybackLoopMode.CONTINUOUS
        }
        _playbackMessage.value = "Minuteur annulé (boucle continue)"
    }

    fun addTimerMinutes(extraMinutes: Int) {
        if (_timerState.value.isActive) {
            val addSec = extraMinutes * 60
            _timerState.value = _timerState.value.copy(
                totalSeconds = _timerState.value.totalSeconds + addSec,
                remainingSeconds = _timerState.value.remainingSeconds + addSec
            )
            _playbackMessage.value = "+$extraMinutes min ajoutées au minuteur"
        } else {
            startSleepTimer(extraMinutes)
        }
    }

    fun clearPlaybackMessage() {
        _playbackMessage.value = null
    }

    fun playSong(song: SongEntity, coverUri: String?, queue: List<SongEntity> = listOf(song)) {
        _currentPlayingSong.value = song
        _currentCoverUri.value = coverUri
        _playbackProgressSec.value = 0
        _isPlaying.value = true
        currentQueue = queue
        currentQueueIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        if (!song.audioUri.isNullOrBlank()) {
            audioPlayerManager.playAudio(song.audioUri) { realDurationSec ->
                // Mise à jour de la durée réelle si le tag était approximatif
                if (realDurationSec > 0 && realDurationSec != song.durationSeconds) {
                    viewModelScope.launch {
                        repository.updateSong(song.copy(durationSeconds = realDurationSec))
                    }
                }
            }
        } else {
            audioPlayerManager.stop()
        }
    }

    fun playAlbum(
        albumWithSongs: AlbumWithSongs,
        mode: PlaybackLoopMode? = null,
        timerMinutes: Int? = null
    ) {
        mode?.let { setLoopMode(it) }
        if (timerMinutes != null && timerMinutes > 0) {
            startSleepTimer(timerMinutes)
        }
        val songs = albumWithSongs.songs.sortedBy { it.trackNumber }
        if (songs.isNotEmpty()) {
            playSong(songs.first(), albumWithSongs.album.coverUri, songs)
        }
    }

    fun playPlaylist(
        playlistWithSongs: PlaylistWithSongs,
        mode: PlaybackLoopMode? = null,
        timerMinutes: Int? = null
    ) {
        mode?.let { setLoopMode(it) }
        if (timerMinutes != null && timerMinutes > 0) {
            startSleepTimer(timerMinutes)
        }
        val songs = playlistWithSongs.items.sortedBy { it.playlistItem.position }.map { it.song }
        if (songs.isNotEmpty()) {
            playSong(songs.first(), playlistWithSongs.playlist.coverUri, songs)
        }
    }

    fun togglePlayPause() {
        val song = _currentPlayingSong.value ?: return
        if (_isPlaying.value) {
            _isPlaying.value = false
            if (!song.audioUri.isNullOrBlank()) {
                audioPlayerManager.pause()
            }
        } else {
            _isPlaying.value = true
            if (!song.audioUri.isNullOrBlank()) {
                audioPlayerManager.resume()
            }
        }
    }

    fun skipNext() {
        if (currentQueue.isNotEmpty()) {
            currentQueueIndex = (currentQueueIndex + 1) % currentQueue.size
            val nextSong = currentQueue[currentQueueIndex]
            playSong(nextSong, _currentCoverUri.value, currentQueue)
            resolveCoverForSong(nextSong)
        }
    }

    fun skipPrevious() {
        if (currentQueue.isNotEmpty()) {
            currentQueueIndex = if (currentQueueIndex > 0) currentQueueIndex - 1 else currentQueue.size - 1
            val prevSong = currentQueue[currentQueueIndex]
            playSong(prevSong, _currentCoverUri.value, currentQueue)
            resolveCoverForSong(prevSong)
        }
    }

    fun seekTo(seconds: Int) {
        val song = _currentPlayingSong.value ?: return
        _playbackProgressSec.value = seconds.coerceIn(0, song.durationSeconds)
        if (!song.audioUri.isNullOrBlank()) {
            audioPlayerManager.seekToSeconds(seconds)
        }
    }

    private fun resolveCoverForSong(song: SongEntity) {
        viewModelScope.launch {
            val album = allAlbums.value.firstOrNull { it.id == song.albumId }
            if (album?.coverUri != null) {
                _currentCoverUri.value = album.coverUri
            }
        }
    }

    // --- Navigation ---
    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            true
        } else {
            false
        }
    }

    fun getAlbumWithSongs(albumId: Long) = repository.getAlbumWithSongs(albumId)

    fun getPlaylistWithSongs(playlistId: Long) = repository.getPlaylistWithSongs(playlistId)

    // --- Importation de fichiers audio (MP3, WAV, etc.) ---
    fun importAudioFilesToAlbum(
        context: Context,
        albumId: Long,
        uris: List<Uri>,
        defaultArtist: String = "Artiste"
    ) {
        viewModelScope.launch {
            val contentResolver = context.contentResolver
            val existingSongs = repository.getSongsForAlbum(albumId)
            var currentTrackCount = 0

            val songsToInsert = mutableListOf<SongEntity>()

            for ((index, uri) in uris.withIndex()) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}

                val meta = AudioImportHelper.extractMetadata(
                    context = context,
                    uri = uri,
                    fallbackArtist = defaultArtist,
                    trackIndex = currentTrackCount + index + 1
                )

                val song = SongEntity(
                    albumId = albumId,
                    title = meta.title,
                    artist = meta.artist,
                    trackNumber = currentTrackCount + index + 1,
                    durationSeconds = meta.durationSeconds,
                    audioUri = uri.toString()
                )
                songsToInsert.add(song)
            }

            if (songsToInsert.isNotEmpty()) {
                repository.addSongs(songsToInsert)
            }
        }
    }

    fun importAudioFilesToPlaylist(
        context: Context,
        playlistId: Long,
        uris: List<Uri>
    ) {
        viewModelScope.launch {
            val contentResolver = context.contentResolver
            // Trouver ou créer un album conteneur pour ces musiques importées
            var targetAlbum = allAlbums.value.firstOrNull { it.title == "Musiques Importées" }
            val albumId = if (targetAlbum != null) {
                targetAlbum.id
            } else {
                repository.createAlbum(
                    title = "Musiques Importées",
                    artist = "Divers Artistes",
                    genre = "Import local",
                    releaseYear = 2024
                )
            }

            val songsToInsert = mutableListOf<SongEntity>()

            for ((index, uri) in uris.withIndex()) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}

                val meta = AudioImportHelper.extractMetadata(
                    context = context,
                    uri = uri,
                    fallbackArtist = "Artiste local",
                    trackIndex = index + 1
                )

                val song = SongEntity(
                    albumId = albumId,
                    title = meta.title,
                    artist = meta.artist,
                    trackNumber = index + 1,
                    durationSeconds = meta.durationSeconds,
                    audioUri = uri.toString()
                )
                songsToInsert.add(song)
            }

            if (songsToInsert.isNotEmpty()) {
                val insertedSongIds = repository.addSongs(songsToInsert)
                // Ajouter chaque chanson insérée dans la playlist à la suite
                insertedSongIds.forEachIndexed { i, songId ->
                    repository.addSongToPlaylist(playlistId, songId, i)
                }
            }
        }
    }

    fun createAlbumWithAudioFiles(
        context: Context,
        title: String,
        artist: String,
        genre: String,
        year: Int,
        coverUri: String?,
        audioUris: List<Uri>
    ) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = context,
                sourceUriStr = coverUri,
                prefix = "album"
            )
            val albumId = repository.createAlbum(title, artist, genre, year, permanentCover)
            if (audioUris.isNotEmpty()) {
                importAudioFilesToAlbum(context, albumId, audioUris, artist)
            }
        }
    }

    // --- Import Images via Photo Picker (Pochettes d'albums & artworks) ---
    fun importCovers(context: Context, uris: List<Uri>, targetAlbumId: Long? = null) {
        viewModelScope.launch {
            val contentResolver = context.contentResolver
            val imagesToInsert = mutableListOf<MediaItemEntity>()

            for (uri in uris) {
                // Sauvegarde persistante dans les fichiers internes de l'application
                val permanentUri = ImageStorageHelper.persistImageToInternalStorage(
                    context = context,
                    sourceUriStr = uri.toString(),
                    prefix = "gallery"
                ) ?: uri.toString()

                val title = queryMetadata(contentResolver, uri)
                val item = MediaItemEntity(
                    uri = permanentUri,
                    title = title,
                    mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                )
                imagesToInsert.add(item)
            }

            if (imagesToInsert.isNotEmpty()) {
                repository.addImages(imagesToInsert)
                if (targetAlbumId != null) {
                    val cover = imagesToInsert.first().uri
                    repository.updateAlbumCover(targetAlbumId, cover)
                    if (_currentPlayingSong.value?.albumId == targetAlbumId) {
                        _currentCoverUri.value = cover
                    }
                }
            }
        }
    }

    private fun queryMetadata(contentResolver: ContentResolver, uri: Uri): String {
        var name = "Pochette_${System.currentTimeMillis()}"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    // --- Albums Musicaux ---
    fun createAlbum(
        title: String,
        artist: String,
        genre: String = "Pop",
        releaseYear: Int = 2024,
        coverUri: String? = null
    ) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = getApplication(),
                sourceUriStr = coverUri,
                prefix = "album"
            )
            repository.createAlbum(title, artist, genre, releaseYear, permanentCover)
        }
    }

    fun updateAlbum(album: AlbumEntity) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = getApplication(),
                sourceUriStr = album.coverUri,
                prefix = "album_${album.id}"
            )
            val updatedAlbum = album.copy(coverUri = permanentCover)
            repository.updateAlbum(updatedAlbum)
            if (_currentPlayingSong.value?.albumId == album.id) {
                _currentCoverUri.value = permanentCover
            }
        }
    }

    fun updateAlbumCover(albumId: Long, coverUri: String?) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = getApplication(),
                sourceUriStr = coverUri,
                prefix = "album_$albumId"
            )
            repository.updateAlbumCover(albumId, permanentCover)
            if (_currentPlayingSong.value?.albumId == albumId) {
                _currentCoverUri.value = permanentCover
            }
        }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            val album = allAlbums.value.firstOrNull { it.id == albumId }
            ImageStorageHelper.deleteInternalImageIfPresent(getApplication(), album?.coverUri)
            repository.deleteAlbum(albumId)
            if (_currentScreen.value is AppScreen.AlbumDetail) {
                navigateBack()
            }
        }
    }

    fun deleteImage(imageId: Long) {
        viewModelScope.launch {
            val item = allImages.value.firstOrNull { it.id == imageId }
            ImageStorageHelper.deleteInternalImageIfPresent(getApplication(), item?.uri)
            repository.deleteImage(imageId)
        }
    }

    // --- Chansons (Songs) ---
    fun addSongToAlbum(
        albumId: Long,
        title: String,
        artist: String,
        durationSeconds: Int = 210,
        trackNumber: Int = 1
    ) {
        viewModelScope.launch {
            repository.addSong(
                albumId = albumId,
                title = title,
                artist = artist,
                trackNumber = trackNumber,
                durationSeconds = durationSeconds
            )
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            if (_currentPlayingSong.value?.id == songId) {
                skipNext()
            }
        }
    }

    // --- Playlists ---
    fun createPlaylist(name: String, description: String = "", coverUri: String? = null) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = getApplication(),
                sourceUriStr = coverUri,
                prefix = "playlist"
            )
            repository.createPlaylist(name, description, permanentCover)
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            val permanentCover = ImageStorageHelper.persistImageToInternalStorage(
                context = getApplication(),
                sourceUriStr = playlist.coverUri,
                prefix = "playlist_${playlist.id}"
            )
            repository.updatePlaylist(playlist.copy(coverUri = permanentCover))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            val playlist = allPlaylists.value.firstOrNull { it.id == playlistId }
            ImageStorageHelper.deleteInternalImageIfPresent(getApplication(), playlist?.coverUri)
            repository.deletePlaylist(playlistId)
            if (_currentScreen.value is AppScreen.PlaylistDetail) {
                navigateBack()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long, currentItemCount: Int) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId, currentItemCount)
        }
    }

    fun removeSongFromPlaylist(playlistItemId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistItemId)
        }
    }

    fun movePlaylistItem(playlistId: Long, items: List<PlaylistItemEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return
        viewModelScope.launch {
            val mutable = items.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            val updated = mutable.mapIndexed { index, item -> item.copy(position = index) }
            repository.reorderPlaylist(playlistId, updated)
        }
    }

    fun reorderAlbumSongs(songs: List<SongEntity>) {
        viewModelScope.launch {
            repository.reorderAlbumSongs(songs)
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, items: List<PlaylistItemEntity>) {
        viewModelScope.launch {
            val updated = items.mapIndexed { index, item -> item.copy(position = index) }
            repository.reorderPlaylist(playlistId, updated)
        }
    }

    // --- Données d'exemples réalistes (Albums musicaux, Chansons et Playlists) ---
    fun loadSampleMusic(context: Context) {
        viewModelScope.launch {
            val pkg = context.packageName
            val cover1 = "android.resource://$pkg/${R.drawable.sample_landscape_1793562015517}"
            val cover2 = "android.resource://$pkg/${R.drawable.sample_coastal_1793562028794}"

            repository.addImages(
                listOf(
                    MediaItemEntity(uri = cover1, title = "Artwork Échos Solaires"),
                    MediaItemEntity(uri = cover2, title = "Artwork Marées Océanes")
                )
            )

            // Album 1 : Échos Solaires
            val album1Id = repository.createAlbum(
                title = "Échos Solaires",
                artist = "Astral Horizon",
                genre = "Synthwave / Électro",
                releaseYear = 2024,
                coverUri = cover1
            )
            val songsAlbum1 = listOf(
                SongEntity(albumId = album1Id, title = "Aurore Boréale", artist = "Astral Horizon", trackNumber = 1, durationSeconds = 222),
                SongEntity(albumId = album1Id, title = "Onde Céleste", artist = "Astral Horizon", trackNumber = 2, durationSeconds = 255),
                SongEntity(albumId = album1Id, title = "Poussière d'Étoiles", artist = "Astral Horizon", trackNumber = 3, durationSeconds = 198),
                SongEntity(albumId = album1Id, title = "Nébuleuse Indigo", artist = "Astral Horizon", trackNumber = 4, durationSeconds = 290)
            )
            val album1SongIds = repository.addSongs(songsAlbum1)

            // Album 2 : Marées Océanes
            val album2Id = repository.createAlbum(
                title = "Marées Océanes",
                artist = "Horizon Bleu",
                genre = "Acoustique / Pop",
                releaseYear = 2023,
                coverUri = cover2
            )
            val songsAlbum2 = listOf(
                SongEntity(albumId = album2Id, title = "Vagues du Crépuscule", artist = "Horizon Bleu", trackNumber = 1, durationSeconds = 235),
                SongEntity(albumId = album2Id, title = "Brise Marine", artist = "Horizon Bleu", trackNumber = 2, durationSeconds = 165),
                SongEntity(albumId = album2Id, title = "Écume Éternelle", artist = "Horizon Bleu", trackNumber = 3, durationSeconds = 260),
                SongEntity(albumId = album2Id, title = "Lueur Côtière", artist = "Horizon Bleu", trackNumber = 4, durationSeconds = 210)
            )
            val album2SongIds = repository.addSongs(songsAlbum2)

            // Playlist
            val playlistId = repository.createPlaylist(
                name = "Mix Détente & Voyage",
                description = "Sélection musicale fluide et séquencée pour écoute continue",
                coverUri = cover1
            )

            val sequence = listOf(
                album1SongIds.getOrNull(0) ?: 1L,
                album2SongIds.getOrNull(0) ?: 5L,
                album1SongIds.getOrNull(1) ?: 2L,
                album2SongIds.getOrNull(1) ?: 6L
            )
            sequence.forEachIndexed { idx, songId ->
                repository.addSongToPlaylist(playlistId, songId, idx)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: MediaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
