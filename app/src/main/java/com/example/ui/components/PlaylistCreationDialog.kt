package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity

/**
 * Boîte de dialogue complète pour créer une playlist :
 * - Choix par albums entiers (multi-sélection d'albums complets)
 * - Choix par chansons ajoutées (multi-sélection titre par titre avec recherche)
 */
@Composable
fun CreatePlaylistWithSelectionDialog(
    albums: List<AlbumEntity>,
    allSongs: List<SongEntity>,
    onConfirm: (name: String, description: String, selectedAlbumIds: List<Long>, selectedSongIds: List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Par albums entiers, 1 = Par chansons

    var selectedAlbumIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var songSearchQuery by remember { mutableStateOf("") }
    var selectedAlbumFilterId by remember { mutableStateOf<Long?>(null) }

    // Calcul du total de titres sélectionnés
    val songsFromAlbums = remember(selectedAlbumIds, allSongs) {
        allSongs.filter { it.albumId in selectedAlbumIds }
    }
    val additionalSongs = remember(selectedSongIds, selectedAlbumIds, allSongs) {
        allSongs.filter { it.id in selectedSongIds && it.albumId !in selectedAlbumIds }
    }
    val totalTracksCount = songsFromAlbums.size + additionalSongs.size

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 680.dp),
        title = {
            Column {
                Text(
                    text = "Créer une Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sélectionnez des albums entiers ou des chansons spécifiques",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Saisie du nom & description
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la playlist *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_create_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnelle)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_create_desc_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Onglets de sélection : Par Albums Entiers vs Par Chansons
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedAlbumIds.isNotEmpty()) "Albums (${selectedAlbumIds.size})" else "Albums entiers",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_by_albums")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedSongIds.isNotEmpty()) "Chansons (${selectedSongIds.size})" else "Par chansons",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_by_songs")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenu de l'onglet actif
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 200.dp, max = 320.dp)
                ) {
                    if (selectedTab == 0) {
                        // Onglet 1 : Sélection d'albums entiers
                        AlbumsSelectionContent(
                            albums = albums,
                            allSongs = allSongs,
                            selectedAlbumIds = selectedAlbumIds,
                            onToggleAlbum = { albumId ->
                                selectedAlbumIds = if (albumId in selectedAlbumIds) {
                                    selectedAlbumIds - albumId
                                } else {
                                    selectedAlbumIds + albumId
                                }
                            },
                            onSelectAll = {
                                selectedAlbumIds = albums.map { it.id }.toSet()
                            },
                            onDeselectAll = {
                                selectedAlbumIds = emptySet()
                            }
                        )
                    } else {
                        // Onglet 2 : Sélection par chansons individuelles
                        SongsSelectionContent(
                            albums = albums,
                            allSongs = allSongs,
                            selectedSongIds = selectedSongIds,
                            selectedAlbumIds = selectedAlbumIds,
                            searchQuery = songSearchQuery,
                            onSearchQueryChange = { songSearchQuery = it },
                            selectedAlbumFilterId = selectedAlbumFilterId,
                            onAlbumFilterChange = { selectedAlbumFilterId = it },
                            onToggleSong = { songId ->
                                selectedSongIds = if (songId in selectedSongIds) {
                                    selectedSongIds - songId
                                } else {
                                    selectedSongIds + songId
                                }
                            },
                            onSelectAllVisible = { visibleSongIds ->
                                selectedSongIds = selectedSongIds + visibleSongIds
                            },
                            onDeselectAll = {
                                selectedSongIds = emptySet()
                            }
                        )
                    }
                }

                // Résumé de la sélection
                if (totalTracksCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildString {
                                    if (selectedAlbumIds.isNotEmpty()) {
                                        append("${selectedAlbumIds.size} album(s) (${songsFromAlbums.size} titres)")
                                    }
                                    if (additionalSongs.isNotEmpty()) {
                                        if (selectedAlbumIds.isNotEmpty()) append(" + ")
                                        append("${additionalSongs.size} titre(s) individuel(s)")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$totalTracksCount au total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            description.trim(),
                            selectedAlbumIds.toList(),
                            selectedSongIds.toList()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("playlist_confirm_create_btn")
            ) {
                Text(
                    text = if (totalTracksCount > 0) "Créer ($totalTracksCount titres)" else "Créer vide"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("playlist_cancel_create_btn")
            ) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Contenu de sélection des albums entiers
 */
@Composable
private fun AlbumsSelectionContent(
    albums: List<AlbumEntity>,
    allSongs: List<SongEntity>,
    selectedAlbumIds: Set<Long>,
    onToggleAlbum: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucun album musical disponible.\nCréez d'abord un album ou importez des musiques.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Barre d'actions rapides (Tout sélectionner / Tout désélectionner)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedAlbumIds.size}/${albums.size} album(s) sélectionné(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                TextButton(
                    onClick = onSelectAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Tout cocher", fontSize = 12.sp)
                }
                if (selectedAlbumIds.isNotEmpty()) {
                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Décocher", fontSize = 12.sp)
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums, key = { it.id }) { album ->
                val isSelected = album.id in selectedAlbumIds
                val trackCount = remember(album.id, allSongs) {
                    allSongs.count { it.albumId == album.id }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleAlbum(album.id) }
                        .testTag("album_select_card_${album.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pochette
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!album.coverUri.isNullOrBlank()) {
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • $trackCount chanson(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleAlbum(album.id) },
                            modifier = Modifier.testTag("checkbox_album_${album.id}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contenu de sélection par chansons individuelles
 */
@Composable
private fun SongsSelectionContent(
    albums: List<AlbumEntity>,
    allSongs: List<SongEntity>,
    selectedSongIds: Set<Long>,
    selectedAlbumIds: Set<Long>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedAlbumFilterId: Long?,
    onAlbumFilterChange: (Long?) -> Unit,
    onToggleSong: (Long) -> Unit,
    onSelectAllVisible: (List<Long>) -> Unit,
    onDeselectAll: () -> Unit
) {
    if (allSongs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucune chanson disponible dans vos albums.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val filteredSongs = remember(allSongs, searchQuery, selectedAlbumFilterId) {
        allSongs.filter { song ->
            val matchesSearch = searchQuery.isBlank() ||
                    song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true)
            val matchesAlbum = selectedAlbumFilterId == null || song.albumId == selectedAlbumFilterId
            matchesSearch && matchesAlbum
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher un titre ou un artiste...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("songs_search_input")
        )

        // Filtre rapide par album
        if (albums.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedAlbumFilterId == null,
                        onClick = { onAlbumFilterChange(null) },
                        label = { Text("Tous", fontSize = 11.sp) }
                    )
                }
                items(albums, key = { it.id }) { album ->
                    FilterChip(
                        selected = selectedAlbumFilterId == album.id,
                        onClick = {
                            onAlbumFilterChange(if (selectedAlbumFilterId == album.id) null else album.id)
                        },
                        label = { Text(album.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        // Actions rapides
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredSongs.size} résultat(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(
                    onClick = { onSelectAllVisible(filteredSongs.map { it.id }) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Tout cocher", fontSize = 11.sp)
                }
                if (selectedSongIds.isNotEmpty()) {
                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Décocher", fontSize = 11.sp)
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSongs, key = { it.id }) { song ->
                val isSelected = song.id in selectedSongIds
                val isAlreadyInSelectedAlbum = song.albumId in selectedAlbumIds

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAlreadyInSelectedAlbum) { onToggleSong(song.id) }
                        .testTag("song_select_card_${song.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected && !isAlreadyInSelectedAlbum) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isAlreadyInSelectedAlbum -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                if (isAlreadyInSelectedAlbum) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Inclus via l'album)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Checkbox(
                            checked = isSelected || isAlreadyInSelectedAlbum,
                            enabled = !isAlreadyInSelectedAlbum,
                            onCheckedChange = { onToggleSong(song.id) },
                            modifier = Modifier.testTag("checkbox_song_${song.id}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Boîte de dialogue pour ajouter des morceaux ou des albums complets à une playlist EXISTANTE
 */
@Composable
fun AddItemsToPlaylistDialog(
    playlistName: String,
    albums: List<AlbumEntity>,
    allSongs: List<SongEntity>,
    onAddAlbums: (albumIds: List<Long>) -> Unit,
    onAddSongs: (songIds: List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Albums entiers, 1 = Chansons
    var selectedAlbumIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var songSearchQuery by remember { mutableStateOf("") }
    var selectedAlbumFilterId by remember { mutableStateOf<Long?>(null) }

    val totalCount = if (selectedTab == 0) {
        allSongs.count { it.albumId in selectedAlbumIds }
    } else {
        selectedSongIds.size
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 640.dp),
        title = {
            Column {
                Text(
                    text = "Ajouter à '$playlistName'",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Choisissez des albums complets ou des morceaux à ajouter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Albums entiers", fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chansons", fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 220.dp, max = 340.dp)
                ) {
                    if (selectedTab == 0) {
                        AlbumsSelectionContent(
                            albums = albums,
                            allSongs = allSongs,
                            selectedAlbumIds = selectedAlbumIds,
                            onToggleAlbum = { albumId ->
                                selectedAlbumIds = if (albumId in selectedAlbumIds) selectedAlbumIds - albumId else selectedAlbumIds + albumId
                            },
                            onSelectAll = { selectedAlbumIds = albums.map { it.id }.toSet() },
                            onDeselectAll = { selectedAlbumIds = emptySet() }
                        )
                    } else {
                        SongsSelectionContent(
                            albums = albums,
                            allSongs = allSongs,
                            selectedSongIds = selectedSongIds,
                            selectedAlbumIds = emptySet(),
                            searchQuery = songSearchQuery,
                            onSearchQueryChange = { songSearchQuery = it },
                            selectedAlbumFilterId = selectedAlbumFilterId,
                            onAlbumFilterChange = { selectedAlbumFilterId = it },
                            onToggleSong = { songId ->
                                selectedSongIds = if (songId in selectedSongIds) selectedSongIds - songId else selectedSongIds + songId
                            },
                            onSelectAllVisible = { visibleIds -> selectedSongIds = selectedSongIds + visibleIds },
                            onDeselectAll = { selectedSongIds = emptySet() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0 && selectedAlbumIds.isNotEmpty()) {
                        onAddAlbums(selectedAlbumIds.toList())
                    } else if (selectedTab == 1 && selectedSongIds.isNotEmpty()) {
                        onAddSongs(selectedSongIds.toList())
                    }
                },
                enabled = (selectedTab == 0 && selectedAlbumIds.isNotEmpty()) || (selectedTab == 1 && selectedSongIds.isNotEmpty()),
                modifier = Modifier.testTag("add_items_confirm_btn")
            ) {
                Text(if (totalCount > 0) "Ajouter ($totalCount titres)" else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Boîte de dialogue pour ajouter UN ALBUM ENTIER à une playlist existante ou en créer une nouvelle
 */
@Composable
fun AddWholeAlbumToPlaylistDialog(
    albumTitle: String,
    playlists: List<PlaylistEntity>,
    onSelectPlaylist: (playlistId: Long) -> Unit,
    onCreateNewPlaylistWithAlbum: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateSubDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("Playlist - $albumTitle") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajouter tout l'album '$albumTitle' à :",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Option pour créer une nouvelle playlist avec cet album
                Button(
                    onClick = { showCreateSubDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("create_new_playlist_for_album_btn")
                ) {
                    Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Créer une nouvelle playlist pour cet album")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "Aucune playlist existante. Cliquez sur le bouton ci-dessus pour en créer une.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Ou ajouter à une playlist existante :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylist(playlist.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Album,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
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

    if (showCreateSubDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSubDialog = false },
            title = { Text("Nouvelle Playlist pour cet album") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nom de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subdialog_new_playlist_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreateNewPlaylistWithAlbum(newPlaylistName.trim())
                            showCreateSubDialog = false
                            onDismiss()
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text("Créer et Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSubDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
