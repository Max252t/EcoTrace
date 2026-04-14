package com.topit.ecotrace.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.topit.ecotrace.presentation.navigation.Screen
import com.topit.ecotrace.presentation.screens.AddReportScreen
import com.topit.ecotrace.presentation.screens.FiltersBottomSheet
import com.topit.ecotrace.presentation.screens.LocationPickerScreen
import com.topit.ecotrace.presentation.screens.MapScreen
import com.topit.ecotrace.presentation.screens.MyReportsScreen
import com.topit.ecotrace.presentation.screens.ProfileScreen
import com.topit.ecotrace.presentation.screens.ReportDetailsScreen
import com.topit.ecotrace.presentation.screens.SettingsScreen
import com.topit.ecotrace.ui.LocalAppStrings

private data class BottomItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: @Composable () -> String,
)

private val hiddenNavRoutes = setOf(
    Screen.AddReport.route.substringBefore("?"),
    Screen.ReportDetails.route.substringBefore("/"),
    Screen.Filters.route,
    Screen.LocationPicker.route.substringBefore("?"),
    Screen.Settings.route,
)

private const val DEFAULT_LAT = 55.751244
private const val DEFAULT_LON = 37.618423

@Composable
fun EcoTraceAppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")?.substringBefore("/")
    val s = LocalAppStrings.current

    val showBottomBar = currentRoute !in hiddenNavRoutes

    val bottomItems = listOf(
        BottomItem(Screen.Map, Icons.Filled.Map, Icons.Outlined.Map) { s.navMap },
        BottomItem(Screen.MyReports, Icons.Filled.Report, Icons.Outlined.Report) { s.navReports },
        BottomItem(Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person) { s.navProfile },
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label(),
                                )
                            },
                            label = { Text(item.label()) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(navController = navController, startDestination = Screen.Map.route) {
            composable(Screen.Map.route) {
                MapScreen(
                    contentPadding = paddingValues,
                    onAddClick = { lat, lon -> navController.navigate(Screen.AddReport.createRoute(lat, lon)) },
                    onFiltersClick = { navController.navigate(Screen.Filters.route) },
                    onReportClick = { id -> navController.navigate(Screen.ReportDetails.createRoute(id)) },
                )
            }
            composable(Screen.MyReports.route) {
                MyReportsScreen(contentPadding = paddingValues)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    contentPadding = paddingValues,
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(contentPadding = paddingValues, onBack = { navController.navigateUp() })
            }

            // ── AddReport: reads picked location from savedStateHandle ─────
            composable(
                route = Screen.AddReport.route,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = "" },
                ),
            ) { back ->
                val initLat = back.arguments?.getString("lat")?.toDoubleOrNull() ?: DEFAULT_LAT
                val initLon = back.arguments?.getString("lon")?.toDoubleOrNull() ?: DEFAULT_LON

                // Updated by LocationPickerScreen via previousBackStackEntry.savedStateHandle
                val pickedLat by back.savedStateHandle
                    .getStateFlow("picked_lat", initLat)
                    .collectAsStateWithLifecycle()
                val pickedLon by back.savedStateHandle
                    .getStateFlow("picked_lon", initLon)
                    .collectAsStateWithLifecycle()

                AddReportScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                    onPickLocation = { currentLat, currentLon ->
                        navController.navigate(
                            Screen.LocationPicker.createRoute(currentLat, currentLon)
                        )
                    },
                    currentLat = pickedLat,
                    currentLon = pickedLon,
                )
            }

            composable(
                route = Screen.ReportDetails.route,
                arguments = listOf(navArgument("reportId") { type = NavType.StringType }),
            ) { back ->
                ReportDetailsScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                    reportId = back.arguments?.getString("reportId").orEmpty(),
                )
            }
            composable(Screen.Filters.route) {
                FiltersBottomSheet(contentPadding = paddingValues, onBack = { navController.navigateUp() })
            }

            // ── LocationPicker: saves result to AddReport's savedStateHandle ─
            composable(
                route = Screen.LocationPicker.route,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = "" },
                ),
            ) { back ->
                val initLat = back.arguments?.getString("lat")?.toDoubleOrNull() ?: DEFAULT_LAT
                val initLon = back.arguments?.getString("lon")?.toDoubleOrNull() ?: DEFAULT_LON

                LocationPickerScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                    initialLat = initLat,
                    initialLon = initLon,
                    onConfirm = { lat, lon ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lat", lat)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lon", lon)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
