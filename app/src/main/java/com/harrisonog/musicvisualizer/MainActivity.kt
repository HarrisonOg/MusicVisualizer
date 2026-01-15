package com.harrisonog.musicvisualizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.harrisonog.musicvisualizer.presentation.common.MiniPlayer
import com.harrisonog.musicvisualizer.presentation.common.RequireAudioPermission
import com.harrisonog.musicvisualizer.presentation.navigation.MusicVisualizerBottomNavigation
import com.harrisonog.musicvisualizer.presentation.navigation.MusicVisualizerNavGraph
import com.harrisonog.musicvisualizer.presentation.navigation.Route
import com.harrisonog.musicvisualizer.presentation.player.PlayerViewModel
import com.harrisonog.musicvisualizer.ui.theme.MusicVisualizerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicVisualizerTheme {
                RequireAudioPermission {
                    MusicVisualizerApp()
                }
            }
        }
    }
}

@Composable
fun MusicVisualizerApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if mini-player should be visible
    // Hide on Player and Visualizer screens
    val showMiniPlayer = currentSong != null &&
            currentDestination?.hierarchy?.none {
                it.route == Route.Player.route || it.route == Route.Visualizer.route
            } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Mini-player above bottom navigation
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = playbackState.isPlaying,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onClick = { navController.navigate(Route.Player.route) }
                    )
                }

                // Bottom navigation
                MusicVisualizerBottomNavigation(navController = navController)
            }
        }
    ) { paddingValues ->
        MusicVisualizerNavGraph(
            navController = navController,
            onNavigateToPlayer = {
                navController.navigate(Route.Player.route)
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}
