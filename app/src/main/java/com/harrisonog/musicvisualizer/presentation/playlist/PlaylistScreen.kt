package com.harrisonog.musicvisualizer.presentation.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harrisonog.musicvisualizer.domain.model.Playlist
import com.harrisonog.musicvisualizer.presentation.common.EmptyState
import com.harrisonog.musicvisualizer.presentation.common.LoadingState
import com.harrisonog.musicvisualizer.presentation.playlist.components.CreatePlaylistDialog
import com.harrisonog.musicvisualizer.presentation.playlist.components.DeletePlaylistDialog
import com.harrisonog.musicvisualizer.presentation.playlist.components.PlaylistItem
import com.harrisonog.musicvisualizer.presentation.playlist.components.PlaylistOptionsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateToPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create playlist"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.playlists.isEmpty() -> {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "No playlists yet",
                        subtitle = "Create a playlist to organize your music"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.playlists,
                            key = { it.id }
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = { onNavigateToPlaylist(playlist.id) },
                                onLongClick = { selectedPlaylist = playlist }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create playlist dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    // Playlist options bottom sheet
    selectedPlaylist?.let { playlist ->
        PlaylistOptionsSheet(
            playlist = playlist,
            onDismiss = { selectedPlaylist = null },
            onPlay = {
                viewModel.playPlaylist(playlist)
                selectedPlaylist = null
            },
            onShuffle = {
                viewModel.playPlaylist(playlist, shuffle = true)
                selectedPlaylist = null
            },
            onRename = {
                playlistToRename = playlist
                selectedPlaylist = null
            },
            onDelete = {
                playlistToDelete = playlist
                selectedPlaylist = null
            }
        )
    }

    // Delete confirmation dialog
    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
            }
        )
    }

    // Rename dialog
    playlistToRename?.let { playlist ->
        CreatePlaylistDialog(
            onDismiss = { playlistToRename = null },
            onCreate = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
            },
            initialName = playlist.name,
            title = "Rename Playlist",
            confirmText = "Rename"
        )
    }
}
