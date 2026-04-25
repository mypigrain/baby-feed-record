package com.example.baby.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { it / 3 }
                ) + fadeIn(tween(200))
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(200),
                    targetOffsetX = { -it / 3 }
                ) + fadeOut(tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { -it / 3 }
                ) + fadeIn(tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(200),
                    targetOffsetX = { it / 3 }
                ) + fadeOut(tween(150))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToHistory = {
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
