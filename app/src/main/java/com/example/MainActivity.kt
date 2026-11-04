package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingScreen
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.CoversGalleryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = MediaRepository(
            albumDao = database.albumDao(),
            songDao = database.songDao(),
            mediaItemDao = database.mediaItemDao(),
            playlistDao = database.playlistDao()
        )
        MainViewModelFactory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val allImages by viewModel.allImages.collectAsStateWithLifecycle()

    val currentPlayingSong by viewModel.currentPlayingSong.collectAsStateWithLifecycle()
    val currentCoverUri by viewModel.currentCoverUri.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progressSec by viewModel.playbackProgressSec.collectAsStateWithLifecycle()
    val loopMode by viewModel.loopMode.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val playbackMessage by viewModel.playbackMessage.collectAsStateWithLifecycle()

    // Charger les albums musicaux d'exemples s'il n'y a pas encore d'album
    LaunchedEffect(Unit) {
        if (allAlbums.isEmpty()) {
            viewModel.loadSampleMusic(context)
        }
    }

    // Vue plein écran Now Playing
    if (currentScreen is AppScreen.NowPlaying) {
        NowPlayingScreen(
            currentSong = currentPlayingSong,
            coverUri = currentCoverUri,
            isPlaying = isPlaying,
            progressSec = progressSec,
            loopMode = loopMode,
            timerState = timerState,
            onTogglePlay = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeek = { viewModel.seekTo(it) },
            onSetLoopMode = { viewModel.setLoopMode(it) },
            onStartTimer = { viewModel.startSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onAddTimerMinutes = { viewModel.addTimerMinutes(it) },
            onClose = { viewModel.navigateBack() }
        )
        return
    }

    val isRootScreen = currentScreen is AppScreen.Albums ||
            currentScreen is AppScreen.Playlists ||
            currentScreen is AppScreen.CoversGallery

    Scaffold(
        topBar = {
            if (isRootScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                AppScreen.Albums -> "Albums Musicaux"
                                AppScreen.Playlists -> "Playlists"
                                AppScreen.CoversGallery -> "Pochettes & Visuels"
                                else -> "MediaFlow"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                // Mini Player discret docké au-dessus de la barre de navigation
                MiniPlayerBar(
                    currentSong = currentPlayingSong,
                    coverUri = currentCoverUri,
                    isPlaying = isPlaying,
                    progressSec = progressSec,
                    loopMode = loopMode,
                    timerState = timerState,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onClick = { viewModel.navigateTo(AppScreen.NowPlaying) }
                )

                if (isRootScreen) {
                    NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                        NavigationBarItem(
                            selected = currentScreen is AppScreen.Albums,
                            onClick = { viewModel.navigateTo(AppScreen.Albums) },
                            icon = { Icon(Icons.Default.Album, contentDescription = "Albums") },
                            label = { Text("Albums") },
                            modifier = Modifier.testTag("nav_albums")
                        )
                        NavigationBarItem(
                            selected = currentScreen is AppScreen.Playlists,
                            onClick = { viewModel.navigateTo(AppScreen.Playlists) },
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Playlists") },
                            label = { Text("Playlists") },
                            modifier = Modifier.testTag("nav_playlists")
                        )
                        NavigationBarItem(
                            selected = currentScreen is AppScreen.CoversGallery,
                            onClick = { viewModel.navigateTo(AppScreen.CoversGallery) },
                            icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pochettes") },
                            label = { Text("Pochettes") },
                            modifier = Modifier.testTag("nav_covers")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (val screen = currentScreen) {
            AppScreen.Albums -> {
                AlbumsScreen(
                    albums = allAlbums,
                    allSongs = allSongs,
                    onAlbumClick = { albumId -> viewModel.navigateTo(AppScreen.AlbumDetail(albumId)) },
                    onCreateAlbum = { title, artist, genre, year, coverUri, audioUris ->
                        viewModel.createAlbumWithAudioFiles(context, title, artist, genre, year, coverUri, audioUris)
                    },
                    onUpdateAlbum = { album -> viewModel.updateAlbum(album) },
                    onDeleteAlbum = { albumId -> viewModel.deleteAlbum(albumId) },
                    onLoadSamples = { viewModel.loadSampleMusic(context) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.Playlists -> {
                PlaylistsScreen(
                    playlists = allPlaylists,
                    onPlaylistClick = { playlistId -> viewModel.navigateTo(AppScreen.PlaylistDetail(playlistId)) },
                    onStartSlideshow = { playlistId ->
                        viewModel.navigateTo(AppScreen.PlaylistDetail(playlistId))
                    },
                    onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                    onUpdatePlaylist = { playlist -> viewModel.updatePlaylist(playlist) },
                    onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.CoversGallery -> {
                CoversGalleryScreen(
                    images = allImages,
                    albums = allAlbums,
                    playlists = allPlaylists,
                    onImportUris = { uris -> viewModel.importCovers(context, uris) },
                    onAssignToAlbum = { albumId, uri -> viewModel.updateAlbumCover(albumId, uri) },
                    onAssignToPlaylist = { playlistId, uri ->
                        val p = allPlaylists.firstOrNull { it.id == playlistId }
                        if (p != null) viewModel.updatePlaylist(p.copy(coverUri = uri))
                    },
                    onDeleteImage = { imageId -> viewModel.deleteImage(imageId) },
                    onLoadSamples = { viewModel.loadSampleMusic(context) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is AppScreen.AlbumDetail -> {
                AlbumDetailScreen(
                    albumId = screen.albumId,
                    viewModel = viewModel,
                    playlists = allPlaylists,
                    onBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is AppScreen.PlaylistDetail -> {
                PlaylistDetailScreen(
                    playlistId = screen.playlistId,
                    viewModel = viewModel,
                    allSongs = allSongs,
                    onBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.NowPlaying -> {
                // Handled above before Scaffold
            }
        }
    }
}
