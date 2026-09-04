package com.topit.ecotrace.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageModelTest {

    @Test
    fun imageModel_resolvesServerPathAgainstBackendUrl() {
        assertEquals(
            "http://10.0.2.2:8080/api/files/a.jpg",
            imageModel("/api/files/a.jpg", baseUrl = "http://10.0.2.2:8080/"),
        )
    }

    @Test
    fun imageModel_resolvesServerPathWhenBaseUrlHasNoTrailingSlash() {
        assertEquals(
            "http://10.0.2.2:8080/api/files/a.jpg",
            imageModel("api/files/a.jpg", baseUrl = "http://10.0.2.2:8080"),
        )
    }

    @Test
    fun imageModel_keepsLocalContentUriUntouched() {
        val uri = "content://media/external/images/1"

        assertEquals(uri, imageModel(uri, baseUrl = "http://10.0.2.2:8080/"))
    }

    @Test
    fun imageModel_keepsAbsoluteHttpUrlUntouched() {
        val url = "https://cdn.example.com/a.jpg"

        assertEquals(url, imageModel(url, baseUrl = "http://10.0.2.2:8080/"))
    }

    @Test
    fun imageModel_returnsNullForMissingPhoto() {
        assertNull(imageModel(null, baseUrl = "http://10.0.2.2:8080/"))
        assertNull(imageModel("   ", baseUrl = "http://10.0.2.2:8080/"))
    }
}
