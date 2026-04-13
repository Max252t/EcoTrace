package com.topit.ecotrace.presentation

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.topit.ecotrace.presentation.navigation.Screen
import com.topit.ecotrace.presentation.screens.AddReportScreen
import com.topit.ecotrace.presentation.screens.FiltersBottomSheet
import com.topit.ecotrace.presentation.screens.LocationPickerScreen
import com.topit.ecotrace.presentation.screens.MapScreen
import com.topit.ecotrace.presentation.screens.MyReportsScreen
import com.topit.ecotrace.presentation.screens.ProfileScreen
import com.topit.ecotrace.presentation.screens.ReportDetailsScreen

@Composable
fun EcoTraceAppRoot() {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.Map, Screen.MyReports, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconsFor(screen), contentDescription = screen.title) },
                        label = { Text(screen.title) },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier,
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    contentPadding = paddingValues,
                    onAddClick = { lat, lon -> navController.navigate(Screen.AddReport.createRoute(lat, lon)) },
                    onFiltersClick = { navController.navigate(Screen.Filters.route) },
                    onReportClick = { reportId ->
                        navController.navigate(Screen.ReportDetails.createRoute(reportId))
                    },
                )
            }
            composable(Screen.MyReports.route) {
                MyReportsScreen(contentPadding = paddingValues)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(contentPadding = paddingValues)
            }
            composable(
                route = Screen.AddReport.route,
                arguments = listOf(
                    navArgument("lat") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                    navArgument("lon") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
                AddReportScreen(
                    contentPadding = paddingValues,
                    onPickLocation = { navController.navigate(Screen.LocationPicker.route) },
                    initialLat = lat,
                    initialLon = lon,
                )
            }
            composable(
                route = Screen.ReportDetails.route,
                arguments = listOf(navArgument("reportId") { type = NavType.StringType }),
            ) { backStackEntry ->
                ReportDetailsScreen(
                    contentPadding = paddingValues,
                    reportId = backStackEntry.arguments?.getString("reportId").orEmpty(),
                )
            }
            composable(Screen.Filters.route) { FiltersBottomSheet(contentPadding = paddingValues) }
            composable(Screen.LocationPicker.route) { LocationPickerScreen(contentPadding = paddingValues) }
        }
    }
}

private fun iconsFor(screen: Screen): ImageVector = when (screen) {
    Screen.Map -> androidx.compose.material.icons.Icons.Default.Place
    Screen.MyReports -> androidx.compose.material.icons.Icons.Default.List
    Screen.Profile -> androidx.compose.material.icons.Icons.Default.Person
    else -> androidx.compose.material.icons.Icons.Default.Info
}
