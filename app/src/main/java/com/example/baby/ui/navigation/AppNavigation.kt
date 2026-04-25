package com.example.baby.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.baby.ui.history.HistoryScreen
import com.example.baby.ui.home.HomeScreen
import com.example.baby.ui.stats.StatsScreen

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "记录", Icons.Default.EditNote)
    data object History : Screen("history", "历史", Icons.Default.Home)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
}

val bottomNavItems = listOf(Screen.Home, Screen.History, Screen.Stats)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Track navigation direction for animations
    var forwardNavigation by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            // Determine animation direction based on tab order
                            val targetIndex = bottomNavItems.indexOfFirst { it.route == screen.route }
                            val currentIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
                            forwardNavigation = targetIndex >= currentIndex

                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    initialOffsetX = { if (forwardNavigation) it else -it }
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    targetOffsetX = { if (forwardNavigation) -it else it }
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    initialOffsetX = { -it }
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    targetOffsetX = { it }
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToHistory = {
                        forwardNavigation = true
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
