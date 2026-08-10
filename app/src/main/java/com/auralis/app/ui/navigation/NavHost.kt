package com.auralis.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auralis.app.data.model.PerformanceMode
import com.auralis.app.ui.screens.EffectsScreen
import com.auralis.app.ui.screens.HomeScreen
import com.auralis.app.ui.screens.PresetsScreen
import com.auralis.app.ui.screens.SettingsScreen
import com.auralis.app.viewmodel.EqualizerViewModel
import com.auralis.app.viewmodel.VisualizationViewModel

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "Home", Icons.Outlined.Equalizer)
    data object Presets : Dest("presets", "Presets", Icons.Outlined.LibraryMusic)
    data object Effects : Dest("effects", "Effects", Icons.Outlined.Tune)
    data object Settings : Dest("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun AuralisNavHost(
    eqViewModel: EqualizerViewModel,
    visViewModel: VisualizationViewModel,
    reducedMotion: Boolean,
) {
    val navController = rememberNavController()
    val items = listOf(Dest.Home, Dest.Presets, Dest.Effects, Dest.Settings)
    val performance by eqViewModel.performance.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    eqViewModel = eqViewModel,
                    visViewModel = visViewModel,
                    reducedMotion = reducedMotion,
                    performance = performance,
                )
            }
            composable(Dest.Presets.route) { PresetsScreen(viewModel = eqViewModel) }
            composable(Dest.Effects.route) { EffectsScreen(viewModel = eqViewModel) }
            composable(Dest.Settings.route) { SettingsScreen(viewModel = eqViewModel) }
        }
    }
}
