package com.topit.ecotrace.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDetailsScreenRouteTest {

    @Test
    fun routeCandidates_returnsExpectedUrisInPriorityOrder() {
        val lat = 55.751244
        val lon = 37.618423

        val result = routeCandidates(lat, lon)

        assertEquals(4, result.size)
        assertEquals(
            "yandexnavi://build_route_on_map?lat_to=55.751244&lon_to=37.618423",
            result[0],
        )
        assertEquals(
            "yandexmaps://maps.yandex.ru/?rtext=~55.751244,37.618423&rtt=pd",
            result[1],
        )
        assertEquals(
            "geo:55.751244,37.618423?q=55.751244,37.618423",
            result[2],
        )
        assertEquals(
            "https://maps.yandex.ru/?rtext=~55.751244,37.618423&rtt=pd",
            result[3],
        )
    }

    @Test
    fun routeCandidates_containsSafeFallbacks() {
        val result = routeCandidates(1.0, 2.0)

        assertTrue(result.any { it.startsWith("geo:") })
        assertTrue(result.any { it.startsWith("https://") })
    }
}
