package com.harrisonog.musicvisualizer.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MusicVisualizerBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(modifier = modifier) {
        Route.bottomNavItems.forEach { route ->
            val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true

            NavigationBarItem(
                icon = {
                    route.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = route.title
                        )
                    }
                },
                label = { Text(route.title) },
                selected = selected,
                onClick = {
                    val startDestination = navController.graph.findStartDestination()
                    if (route.route == startDestination.route) {
                        // For start destination, pop back to it directly
                        navController.popBackStack(startDestination.id, inclusive = false)
                    } else {
                        navController.navigate(route.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(startDestination.id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
