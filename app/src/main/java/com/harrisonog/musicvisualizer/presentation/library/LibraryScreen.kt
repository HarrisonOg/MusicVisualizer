package com.harrisonog.musicvisualizer.presentation.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.presentation.common.EmptyState
import com.harrisonog.musicvisualizer.presentation.common.LoadingState
import com.harrisonog.musicvisualizer.presentation.library.components.SongItem
import com.harrisonog.musicvisualizer.presentation.library.components.SongOptionsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearch = { searchActive = false },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    placeholder = { Text("Search songs...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        } else {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                }
                                SortDropdownMenu(
                                    expanded = showSortMenu,
                                    currentOption = uiState.sortOption,
                                    onDismiss = { showSortMenu = false },
                                    onSelect = { option ->
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            },
            expanded = searchActive,
            onExpandedChange = { searchActive = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (searchActive) 0.dp else 16.dp)
        ) {
            // Search suggestions could go here
        }

        // Content
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refreshLibrary,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.songs.isEmpty() -> {
                    LoadingState()
                }

                uiState.songs.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.MusicOff,
                        title = if (uiState.searchQuery.isNotEmpty()) {
                            "No songs found"
                        } else {
                            "No music found"
                        },
                        subtitle = if (uiState.searchQuery.isNotEmpty()) {
                            "Try a different search term"
                        } else {
                            "Add music to your device to see it here"
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    viewModel.playSong(song)
                                    onNavigateToPlayer()
                                },
                                onLongClick = {
                                    selectedSong = song
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Song options bottom sheet
    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            onDismiss = { selectedSong = null },
            onPlayNext = {
                viewModel.addToQueue(song)
            },
            onAddToQueue = {
                viewModel.addToQueue(song)
            },
            onAddToPlaylist = {
                // TODO: Show add to playlist dialog
            }
        )
    }
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentOption: SortOption,
    onDismiss: () -> Unit,
    onSelect: (SortOption) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        SortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = when (option) {
                            SortOption.TITLE -> "Title"
                            SortOption.ARTIST -> "Artist"
                            SortOption.ALBUM -> "Album"
                            SortOption.DURATION -> "Duration"
                        },
                        color = if (option == currentOption) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = { onSelect(option) }
            )
        }
    }
}
