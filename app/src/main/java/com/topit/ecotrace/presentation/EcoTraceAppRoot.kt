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

private data class BottomItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomItems = listOf(
    BottomItem(Screen.Map, Icons.Filled.Map, Icons.Outlined.Map),
    BottomItem(Screen.MyReports, Icons.Filled.Report, Icons.Outlined.Report),
    BottomItem(Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person),
)

// Routes where the bottom bar should be hidden
private val hiddenNavRoutes = setOf(
    Screen.AddReport.route.substringBefore("?"),
    Screen.ReportDetails.route.substringBefore("/"),
    Screen.Filters.route,
    Screen.LocationPicker.route,
)

@Composable
fun EcoTraceAppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")?.substringBefore("/")

    val showBottomBar = currentRoute !in hiddenNavRoutes

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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.screen.title,
                                )
                            },
                            label = { Text(item.screen.title) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    contentPadding = paddingValues,
                    onAddClick = { lat, lon -> navController.navigate(Screen.AddReport.createRoute(lat, lon)) },
                    onFiltersClick = { navController.navigate(Screen.Filters.route) },
                    onReportClick = { reportId -> navController.navigate(Screen.ReportDetails.createRoute(reportId)) },
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
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = "" },
                ),
            ) { back ->
                val lat = back.arguments?.getString("lat")?.toDoubleOrNull()
                val lon = back.arguments?.getString("lon")?.toDoubleOrNull()
                AddReportScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                    onPickLocation = { navController.navigate(Screen.LocationPicker.route) },
                    initialLat = lat,
                    initialLon = lon,
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
                FiltersBottomSheet(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(Screen.LocationPicker.route) {
                LocationPickerScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.navigateUp() },
                )
            }
        }
    }
}
