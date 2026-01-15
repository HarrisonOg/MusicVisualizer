package com.harrisonog.musicvisualizer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.harrisonog.musicvisualizer.presentation.library.LibraryScreen
import com.harrisonog.musicvisualizer.presentation.player.PlayerScreen
import com.harrisonog.musicvisualizer.presentation.playlist.PlaylistDetailScreen
import com.harrisonog.musicvisualizer.presentation.playlist.PlaylistScreen
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerScreen

@Composable
fun MusicVisualizerNavGraph(
    navController: NavHostController,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Library.route,
        modifier = modifier
    ) {
        composable(Route.Library.route) {
            LibraryScreen(
                onNavigateToPlayer = onNavigateToPlayer
            )
        }

        composable(Route.Player.route) {
            PlayerScreen()
        }

        composable(Route.Visualizer.route) {
            VisualizerScreen()
        }

        composable(Route.Playlists.route) {
            PlaylistScreen(
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Route.PlaylistDetail.createRoute(playlistId))
                }
            )
        }

        composable(
            route = Route.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = onNavigateToPlayer
            )
        }
    }
}
