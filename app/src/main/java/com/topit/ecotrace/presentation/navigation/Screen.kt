package com.topit.ecotrace.presentation.navigation

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object MyReports : Screen("my_reports")
    data object Profile : Screen("profile")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object AddReport : Screen("add_report?lat={lat}&lon={lon}") {
        fun createRoute(lat: Double? = null, lon: Double? = null): String {
            return if (lat != null && lon != null) {
                "add_report?lat=$lat&lon=$lon"
            } else {
                "add_report?lat=&lon="
            }
        }
    }
    data object ReportDetails : Screen("report_details/{reportId}") {
        fun createRoute(reportId: String): String = "report_details/$reportId"
    }
    data object Filters : Screen("filters")
    data object LocationPicker : Screen("location_picker?lat={lat}&lon={lon}") {
        fun createRoute(lat: Double, lon: Double): String = "location_picker?lat=$lat&lon=$lon"
    }
    data object Settings : Screen("settings")
}
