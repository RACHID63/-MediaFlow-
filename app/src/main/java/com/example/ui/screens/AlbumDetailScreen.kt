package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import com.example.ui.components.DraggableReorderableItem
import com.example.ui.components.rememberReorderableList
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackLoopMode
import com.example.ui.components.SleepTimerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MainViewModel,
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val albumWithSongs by viewModel.getAlbumWithSongs(albumId).collectAsStateWithLifecycle(initialValue = null)
    var showAddSongDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddAlbumToPlaylistDialog by remember { mutableStateOf(false) }
    val loopMode by viewModel.loopMode.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateAlbumCover(albumId, uri.toString())
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && albumWithSongs?.album != null) {
            viewModel.importAudioFilesToAlbum(
                context = context,
                albumId = albumId,
                uris = uris,
                defaultArtist = albumWithSongs!!.album.artist
            )
        }
    }

    val album = albumWithSongs?.album
    val songs = remember(albumWithSongs) {
        albumWithSongs?.songs?.sortedBy { it.trackNumber } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = album?.title ?: "Détails de l'album",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("album_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.deleteAlbum(albumId) },
                        modifier = Modifier.testTag("delete_album_btn")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer l'album",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (album != null) {
                // Carte d'en-tête de l'album musical
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pochette avec bouton pour la changer via Photo Picker
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (album.coverUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(album.coverUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${album.genre} • ${album.releaseYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${songs.size} chansons",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Boutons d'action : Écouter l'album, Ajouter à playlist & Changer pochette
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                albumWithSongs?.let { viewModel.playAlbum(it) }
                            },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1.1f).testTag("play_album_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Écouter")
                        }

                        OutlinedButton(
                            onClick = { showAddAlbumToPlaylistDialog = true },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1.1f).testTag("album_add_to_playlist_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Playlist")
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(0.9f).testTag("album_change_cover_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pochette")
                        }
                    }

                    // Sélecteur de rotation : 1 fois, boucle continue, timeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rotation :",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = loopMode == PlaybackLoopMode.ONCE,
                            onClick = { viewModel.setLoopMode(PlaybackLoopMode.ONCE) },
                            label = { Text("1 fois", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        )
                        FilterChip(
                            selected = loopMode == PlaybackLoopMode.CONTINUOUS,
                            onClick = { viewModel.setLoopMode(PlaybackLoopMode.CONTINUOUS) },
                            label = { Text("Boucle", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        )
                        FilterChip(
                            selected = loopMode == PlaybackLoopMode.TIMED_CONTINUOUS,
                            onClick = { showSleepTimerDialog = true },
                            label = {
                                Text(
                                    if (timerState.isActive) "⏱ ${timerState.formattedRemaining()}" else "Timeline",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section liste des chansons (Tracklist)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pistes / Chansons",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            audioPickerLauncher.launch(
                                arrayOf("audio/*", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/mp3", "audio/flac", "audio/ogg")
                            )
                        },
                        modifier = Modifier.testTag("import_audio_btn")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importer MP3/WAV")
                    }

                    FilledTonalButton(
                        onClick = { showAddSongDialog = true },
                        modifier = Modifier.testTag("add_song_to_album_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucune chanson dans cet album",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Importez vos fichiers audio (MP3, WAV...) depuis votre appareil ou ajoutez une piste manuellement.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                audioPickerLauncher.launch(
                                    arrayOf("audio/*", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/mp3", "audio/flac", "audio/ogg")
                                )
                            }
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importer des fichiers audio (MP3/WAV)")
                        }
                    }
                }
            } else {
                val reorderController = rememberReorderableList(
                    items = songs,
                    keyOf = { it.id }
                ) { reorderedSongs ->
                    viewModel.reorderAlbumSongs(reorderedSongs)
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Appuyez longuement sur une piste pour la glisser et la réordonner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    itemsIndexed(reorderController.list, key = { _, song -> song.id }) { index, song ->
                        DraggableReorderableItem(
                            itemKey = song.id,
                            controller = reorderController
                        ) { isDragging ->
                            SongRowItem(
                                song = song,
                                index = index + 1,
                                coverUri = album?.coverUri,
                                playlists = playlists,
                                isDragging = isDragging,
                                onPlay = {
                                    viewModel.playSong(song, album?.coverUri, reorderController.list)
                                },
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addSongToPlaylist(playlistId, song.id, 0)
                                },
                                onDelete = {
                                    viewModel.deleteSong(song.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSongDialog && album != null) {
        AddSongDialog(
            defaultArtist = album.artist,
            nextTrackNumber = songs.size + 1,
            onConfirm = { title, artist, durationSec, trackNum ->
                viewModel.addSongToAlbum(
                    albumId = albumId,
                    title = title,
                    artist = artist,
                    durationSeconds = durationSec,
                    trackNumber = trackNum
                )
                showAddSongDialog = false
            },
            onDismiss = { showAddSongDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentTimerState = timerState,
            onStartTimer = { mins ->
                viewModel.startSleepTimer(mins)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showAddAlbumToPlaylistDialog && album != null) {
        com.example.ui.components.AddWholeAlbumToPlaylistDialog(
            albumTitle = album.title,
            playlists = playlists,
            onSelectPlaylist = { playlistId ->
                viewModel.addAlbumToPlaylist(albumId, playlistId)
                showAddAlbumToPlaylistDialog = false
            },
            onCreateNewPlaylistWithAlbum = { playlistName ->
                viewModel.createPlaylistWithSelection(
                    name = playlistName,
                    description = "Album complet : ${album.title} - ${album.artist}",
                    coverUri = album.coverUri,
                    albumIds = listOf(albumId)
                )
                showAddAlbumToPlaylistDialog = false
            },
            onDismiss = { showAddAlbumToPlaylistDialog = false }
        )
    }
}

@Composable
fun SongRowItem(
    song: SongEntity,
    index: Int,
    coverUri: String?,
    playlists: List<PlaylistEntity>,
    isDragging: Boolean = false,
    onPlay: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isDragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numéro de piste
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${song.artist} • ${song.formattedDuration()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!song.audioUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isInternal = song.audioUri.startsWith("file://")
                        Text(
                            text = if (isInternal) "• Stocké localement" else "• Audio lié",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Bouton écouter
            IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Lire", tint = MaterialTheme.colorScheme.primary)
            }

            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Glisser-déposer par appui long",
                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(26.dp).padding(horizontal = 2.dp)
            )

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Ajouter à une playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showPlaylistPicker = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer la chanson") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Ajouter '${song.title}' à :") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Aucune playlist disponible. Créez-en une dans l'onglet Playlists.")
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToPlaylist(playlist.id)
                                        showPlaylistPicker = false
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AddSongDialog(
    defaultArtist: String,
    nextTrackNumber: Int,
    onConfirm: (title: String, artist: String, durationSec: Int, trackNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf(defaultArtist) }
    var durationText by remember { mutableStateOf("3:30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une chanson") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la chanson") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_song_title")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artiste") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Durée (ex: 3:30 ou en secondes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val durationSec = parseDuration(durationText)
                        onConfirm(title.trim(), artist.trim(), durationSec, nextTrackNumber)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_confirm_add_song")
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

fun parseDuration(input: String): Int {
    return try {
        if (input.contains(":")) {
            val parts = input.split(":")
            val min = parts[0].trim().toIntOrNull() ?: 3
            val sec = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            min * 60 + sec
        } else {
            input.trim().toIntOrNull() ?: 210
        }
    } catch (_: Exception) {
        210
    }
}
