package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import com.example.ui.components.DraggableReorderableItem
import com.example.ui.components.rememberReorderableList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaylistItemWithSong
import com.example.data.model.SongEntity
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MainViewModel,
    allSongs: List<SongEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val playlistWithSongs by viewModel.getPlaylistWithSongs(playlistId).collectAsStateWithLifecycle(initialValue = null)
    var showAddSongDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFilesToPlaylist(context, playlistId, uris)
        }
    }

    val playlist = playlistWithSongs?.playlist
    val items = remember(playlistWithSongs) {
        playlistWithSongs?.items?.sortedBy { it.playlistItem.position } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "Détails de la playlist",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("playlist_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.deletePlaylist(playlistId) },
                        modifier = Modifier.testTag("delete_playlist_action")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer la playlist",
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
            // Bannière de la playlist avec bouton de lecture continue
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = playlist?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!playlist?.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist?.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                playlistWithSongs?.let { viewModel.playPlaylist(it) }
                            },
                            enabled = items.isNotEmpty(),
                            modifier = Modifier.weight(1.3f).testTag("start_continuous_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lecture continue")
                        }

                        FilledTonalButton(
                            onClick = {
                                audioPickerLauncher.launch(
                                    arrayOf("audio/*", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/mp3", "audio/flac", "audio/ogg")
                                )
                            },
                            modifier = Modifier.weight(1.1f).testTag("playlist_import_audio_btn")
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importer")
                        }

                        FilledTonalButton(
                            onClick = { showAddSongDialog = true },
                            modifier = Modifier.weight(0.9f).testTag("playlist_add_song_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Albums")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
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
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucune chanson dans cette playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Importez directement des musiques (MP3, WAV...) depuis votre appareil ou sélectionnez des chansons de vos albums.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    audioPickerLauncher.launch(
                                        arrayOf("audio/*", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/mp3", "audio/flac", "audio/ogg")
                                    )
                                }
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importer MP3/WAV")
                            }

                            FilledTonalButton(onClick = { showAddSongDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Depuis albums")
                            }
                        }
                    }
                }
            } else {
                val reorderController = rememberReorderableList(items) { reorderedItems ->
                    viewModel.reorderPlaylistSongs(playlistId, reorderedItems.map { it.playlistItem })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Séquence ordonnée pour lecture continue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                text = "Appuyez longuement sur une chanson pour la déplacer et réordonner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    itemsIndexed(reorderController.currentList, key = { _, item -> item.playlistItem.id }) { index, itemWithSong ->
                        DraggableReorderableItem(
                            index = index,
                            controller = reorderController
                        ) { isDragging ->
                            PlaylistItemRow(
                                index = index,
                                totalCount = reorderController.currentList.size,
                                item = itemWithSong,
                                isDragging = isDragging,
                                onPlay = {
                                    viewModel.playSong(
                                        song = itemWithSong.song,
                                        coverUri = playlist?.coverUri,
                                        queue = reorderController.currentList.map { it.song }
                                    )
                                },
                                onMoveUp = {
                                    viewModel.movePlaylistItem(
                                        playlistId = playlistId,
                                        items = reorderController.currentList.map { it.playlistItem },
                                        fromIndex = index,
                                        toIndex = index - 1
                                    )
                                },
                                onMoveDown = {
                                    viewModel.movePlaylistItem(
                                        playlistId = playlistId,
                                        items = reorderController.currentList.map { it.playlistItem },
                                        fromIndex = index,
                                        toIndex = index + 1
                                    )
                                },
                                onRemove = {
                                    viewModel.removeSongFromPlaylist(itemWithSong.playlistItem.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSongDialog) {
        SelectSongForPlaylistDialog(
            allSongs = allSongs,
            onSongSelected = { song ->
                viewModel.addSongToPlaylist(playlistId, song.id, items.size)
                showAddSongDialog = false
            },
            onDismiss = { showAddSongDialog = false }
        )
    }
}

@Composable
fun PlaylistItemRow(
    index: Int,
    totalCount: Int,
    item: PlaylistItemWithSong,
    isDragging: Boolean = false,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isDragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("playlist_item_$index")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            // Numéro de position dans la séquence
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.song.artist} • ${item.song.formattedDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indicateur et poignée de déplacement
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Glisser-déposer par appui long",
                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(26.dp)
                    .padding(horizontal = 2.dp)
            )

            // Boutons de réordonnancement séquentiel
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(32.dp).testTag("move_up_$index")
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(32.dp).testTag("move_down_$index")
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp).testTag("remove_item_$index")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Retirer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SelectSongForPlaylistDialog(
    allSongs: List<SongEntity>,
    onSongSelected: (SongEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une chanson à la playlist") },
        text = {
            if (allSongs.isEmpty()) {
                Text("Aucune chanson disponible dans vos albums. Créez d'abord des albums avec des chansons.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(allSongs.size) { i ->
                        val song = allSongs[i]
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongSelected(song) }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${song.artist} • ${song.formattedDuration()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
