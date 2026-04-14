package com.topit.ecotrace.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    data object Map : Screen("map", "Карта")
    data object MyReports : Screen("my_reports", "Мои отчеты")
    data object Profile : Screen("profile", "Профиль")
    data object AddReport : Screen("add_report?lat={lat}&lon={lon}", "Добавить") {
        fun createRoute(lat: Double? = null, lon: Double? = null): String {
            return if (lat != null && lon != null) {
                "add_report?lat=$lat&lon=$lon"
            } else {
                "add_report?lat=&lon="
            }
        }
    }
    data object ReportDetails : Screen("report_details/{reportId}", "Детали") {
        fun createRoute(reportId: String): String = "report_details/$reportId"
    }
    data object Filters : Screen("filters", "Фильтры")
    data object LocationPicker : Screen("location_picker", "Выбор точки")
    data object Settings : Screen("settings", "Настройки")
}
