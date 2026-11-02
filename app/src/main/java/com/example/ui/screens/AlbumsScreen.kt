package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumEntity
import com.example.data.model.SongEntity

@Composable
fun AlbumsScreen(
    albums: List<AlbumEntity>,
    allSongs: List<SongEntity>,
    onAlbumClick: (Long) -> Unit,
    onCreateAlbum: (title: String, artist: String, genre: String, year: Int, coverUri: String?, audioUris: List<Uri>) -> Unit,
    onUpdateAlbum: (AlbumEntity) -> Unit,
    onDeleteAlbum: (Long) -> Unit,
    onLoadSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToEdit by remember { mutableStateOf<AlbumEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 70.dp).testTag("fab_create_music_album")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvel Album Musical")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // En-tête informatif
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Albums Musicaux",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vos albums, morceaux MP3/WAV et pochettes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onLoadSamples,
                    modifier = Modifier.testTag("load_music_samples_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exemples")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (albums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 80.dp)
                    ) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun album musical pour le moment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Créez un album musical avec le bouton '+' en important vos morceaux MP3/WAV et leur pochette.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLoadSamples) {
                            Text("Charger des albums d'exemples")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albums, key = { it.id }) { album ->
                        val songsForThisAlbum = allSongs.filter { it.albumId == album.id }

                        MusicAlbumCard(
                            album = album,
                            songsCount = songsForThisAlbum.size,
                            onClick = { onAlbumClick(album.id) },
                            onEdit = { albumToEdit = album },
                            onDelete = { onDeleteAlbum(album.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditMusicAlbumDialog(
            title = "Créer un album musical",
            confirmButtonText = "Créer",
            allowAudioImport = true,
            onConfirm = { title, artist, genre, year, coverUri, audioUris ->
                onCreateAlbum(title, artist, genre, year, coverUri, audioUris)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    albumToEdit?.let { album ->
        CreateOrEditMusicAlbumDialog(
            title = "Modifier l'album musical",
            initialTitle = album.title,
            initialArtist = album.artist,
            initialGenre = album.genre,
            initialYear = album.releaseYear,
            initialCoverUri = album.coverUri,
            confirmButtonText = "Enregistrer",
            allowAudioImport = false,
            onConfirm = { title, artist, genre, year, coverUri, _ ->
                onUpdateAlbum(
                    album.copy(
                        title = title,
                        artist = artist,
                        genre = genre,
                        releaseYear = year,
                        coverUri = coverUri
                    )
                )
                albumToEdit = null
            },
            onDismiss = { albumToEdit = null }
        )
    }
}

@Composable
fun MusicAlbumCard(
    album: AlbumEntity,
    songsCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("album_card_${album.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.primaryContainer)
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
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$songsCount chanson(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${album.releaseYear}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp).testTag("album_menu_${album.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
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
    }
}

@Composable
fun CreateOrEditMusicAlbumDialog(
    title: String,
    initialTitle: String = "",
    initialArtist: String = "",
    initialGenre: String = "Pop",
    initialYear: Int = 2024,
    initialCoverUri: String? = null,
    allowAudioImport: Boolean = true,
    confirmButtonText: String = "Valider",
    onConfirm: (title: String, artist: String, genre: String, year: Int, coverUri: String?, audioUris: List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    var albumTitle by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var genre by remember { mutableStateOf(initialGenre) }
    var yearText by remember { mutableStateOf(initialYear.toString()) }
    var coverUri by remember { mutableStateOf(initialCoverUri) }
    var selectedAudioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Sélecteur d'image pour la pochette
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri.toString()
        }
    }

    // Sélecteur de fichiers audio (MP3, WAV, FLAC, M4A...)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedAudioUris = uris
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("Titre de l'album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_album_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artiste / Groupe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_album_artist_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { char -> char.isDigit() } },
                        label = { Text("Année") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bouton pour sélectionner la pochette d'album via Photo Picker
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_album_pick_cover_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (coverUri != null) "Pochette sélectionnée ✓" else "Pochette (Photo Picker)")
                }

                if (allowAudioImport) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bouton pour importer directement des morceaux audio MP3 ou WAV
                    OutlinedButton(
                        onClick = {
                            audioPickerLauncher.launch(
                                arrayOf(
                                    "audio/*",
                                    "audio/mpeg",
                                    "audio/wav",
                                    "audio/x-wav",
                                    "audio/mp3",
                                    "audio/flac",
                                    "audio/ogg"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_album_pick_audio_btn")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (selectedAudioUris.isNotEmpty()) "${selectedAudioUris.size} musique(s) sélectionnée(s) ✓"
                            else "Importer musiques (MP3, WAV...)"
                        )
                    }
                }

                if (coverUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Aperçu pochette",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (albumTitle.isNotBlank() && artist.isNotBlank()) {
                        val parsedYear = yearText.toIntOrNull() ?: 2024
                        onConfirm(
                            albumTitle.trim(),
                            artist.trim(),
                            genre.trim(),
                            parsedYear,
                            coverUri,
                            selectedAudioUris
                        )
                    }
                },
                enabled = albumTitle.isNotBlank() && artist.isNotBlank(),
                modifier = Modifier.testTag("dialog_album_confirm_btn")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
