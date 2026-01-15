package com.harrisonog.musicvisualizer.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harrisonog.musicvisualizer.presentation.common.EmptyState
import com.harrisonog.musicvisualizer.presentation.player.components.AlbumArtDisplay
import com.harrisonog.musicvisualizer.presentation.player.components.PlayerControls
import com.harrisonog.musicvisualizer.presentation.player.components.QueueSheet
import com.harrisonog.musicvisualizer.presentation.player.components.SeekBar
import com.harrisonog.musicvisualizer.presentation.player.components.ShuffleRepeatControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    var showQueue by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                actions = {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        val song = currentSong

        if (song == null) {
            EmptyState(
                icon = Icons.Default.MusicNote,
                title = "No song playing",
                subtitle = "Select a song from your library to start playing",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Album art
                AlbumArtDisplay(
                    albumArtUri = song.albumArtUri,
                    albumName = song.album,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Song info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Seek bar
                SeekBar(
                    progress = playbackState.progress,
                    duration = playbackState.duration,
                    currentPosition = playbackState.currentPosition,
                    onSeek = { progress -> viewModel.seekToProgress(progress) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Player controls
                PlayerControls(
                    isPlaying = playbackState.isPlaying,
                    hasPrevious = true,
                    hasNext = true,
                    onPlayPauseClick = viewModel::togglePlayPause,
                    onPreviousClick = viewModel::skipPrevious,
                    onNextClick = viewModel::skipNext
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shuffle/Repeat controls
                ShuffleRepeatControls(
                    shuffleEnabled = playbackState.shuffleEnabled,
                    repeatMode = playbackState.repeatMode,
                    onShuffleClick = viewModel::toggleShuffle,
                    onRepeatClick = viewModel::cycleRepeatMode
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Queue bottom sheet
    if (showQueue) {
        QueueSheet(
            queue = queue,
            currentSong = currentSong,
            onDismiss = { showQueue = false },
            onSongClick = { index ->
                // Play song at index
                showQueue = false
            },
            onRemoveClick = { index ->
                viewModel.removeFromQueue(index)
            }
        )
    }
}
