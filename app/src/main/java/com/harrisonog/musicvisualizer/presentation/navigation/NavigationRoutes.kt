package com.harrisonog.musicvisualizer.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the app.
 */
sealed class Route(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Library : Route(
        route = "library",
        title = "Library",
        icon = Icons.Default.LibraryMusic
    )

    data object Player : Route(
        route = "player",
        title = "Player",
        icon = Icons.Default.PlayCircle
    )

    data object Visualizer : Route(
        route = "visualizer",
        title = "Visualizer",
        icon = Icons.Default.Equalizer
    )

    data object Playlists : Route(
        route = "playlists",
        title = "Playlists",
        icon = Icons.AutoMirrored.Filled.QueueMusic
    )

    data object PlaylistDetail : Route(
        route = "playlist/{playlistId}",
        title = "Playlist"
    ) {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    companion object {
        val bottomNavItems = listOf(Library, Player, Visualizer, Playlists)
    }
}
