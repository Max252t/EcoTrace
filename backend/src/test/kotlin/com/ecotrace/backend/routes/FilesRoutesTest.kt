package com.ecotrace.backend.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.plugins.configureSerialization
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesRoutesTest {

    private val directory: File = Files.createTempDirectory("ecotrace-files-route").toFile()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun upload_storesFileAndServesItBack() = testApplication {
        installFileRoutes()

        val response = uploadPhoto(PHOTO, "image/png", "photo.png")

        assertEquals(HttpStatusCode.Created, response.status)
        val url = response.bodyAsText().substringAfter("\"url\": \"").substringBefore('"')
        assertTrue(url.startsWith("/api/files/"))

        val stored = File(directory, url.removePrefix("/api/files/"))
        assertContentEquals(PHOTO, stored.readBytes())

        val downloaded = client.get(url)
        assertEquals(HttpStatusCode.OK, downloaded.status)
        assertContentEquals(PHOTO, downloaded.readRawBytes())
    }

    @Test
    fun upload_requiresAuthentication() = testApplication {
        installFileRoutes()

        val response = client.post(FileStorage.ROUTE) {
            setBody(multipart(PHOTO, "image/png", "photo.png"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun upload_rejectsUnsupportedType() = testApplication {
        installFileRoutes()

        val response = uploadPhoto(PHOTO, "application/pdf", "document.pdf")

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun upload_rejectsFileOverTheLimit() = testApplication {
        installFileRoutes(maxFileSizeBytes = 4)

        val response = uploadPhoto(ByteArray(16), "image/png", "photo.png")

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun upload_failsWithoutFilePart() = testApplication {
        installFileRoutes()

        val response = client.post(FileStorage.ROUTE) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            setBody(MultiPartFormDataContent(formData { append("title", "no file here") }))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun download_returnsNotFoundForUnknownFile() = testApplication {
        installFileRoutes()

        assertEquals(HttpStatusCode.NotFound, client.get("${FileStorage.ROUTE}/missing.jpg").status)
    }

    private fun ApplicationTestBuilder.installFileRoutes(maxFileSizeBytes: Long = 1024 * 1024) {
        val storage = FileStorage(directory, maxFileSizeBytes)
        application {
            configureSerialization()
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(JWT.require(Algorithm.HMAC256(SECRET)).withIssuer(ISSUER).build())
                    validate { credential -> JWTPrincipal(credential.payload) }
                }
            }
            routing {
                filesRoutes(storage)
                staticFiles(FileStorage.ROUTE, storage.directory)
            }
        }
    }

    private suspend fun ApplicationTestBuilder.uploadPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ) = client.post(FileStorage.ROUTE) {
        header(HttpHeaders.Authorization, "Bearer ${token()}")
        setBody(multipart(bytes, contentType, fileName))
    }

    private fun multipart(bytes: ByteArray, contentType: String, fileName: String) =
        MultiPartFormDataContent(
            formData {
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    },
                )
            },
        )

    private fun token(): String = JWT.create()
        .withIssuer(ISSUER)
        .withClaim("userId", "user-1")
        .sign(Algorithm.HMAC256(SECRET))

    private companion object {
        const val SECRET = "test-secret"
        const val ISSUER = "ecotrace"
        val PHOTO = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    }
}
