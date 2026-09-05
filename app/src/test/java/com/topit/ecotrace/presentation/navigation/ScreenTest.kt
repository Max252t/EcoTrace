package com.topit.ecotrace.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {

    @Test
    fun addReport_createRouteWithCoordinatesFillsLatAndLon() {
        assertEquals(
            "add_report?lat=55.75&lon=37.61",
            Screen.AddReport.createRoute(lat = 55.75, lon = 37.61),
        )
    }

    @Test
    fun addReport_createRouteWithoutCoordinatesLeavesThemBlank() {
        assertEquals("add_report?lat=&lon=", Screen.AddReport.createRoute())
    }

    @Test
    fun addReport_createRouteRequiresBothCoordinates() {
        assertEquals("add_report?lat=&lon=", Screen.AddReport.createRoute(lat = 55.75, lon = null))
        assertEquals("add_report?lat=&lon=", Screen.AddReport.createRoute(lat = null, lon = 37.61))
    }

    @Test
    fun reportDetails_createRouteEmbedsReportId() {
        assertEquals("report_details/report-1", Screen.ReportDetails.createRoute("report-1"))
    }

    @Test
    fun locationPicker_createRouteEmbedsCoordinates() {
        assertEquals(
            "location_picker?lat=55.75&lon=37.61",
            Screen.LocationPicker.createRoute(lat = 55.75, lon = 37.61),
        )
    }
}
