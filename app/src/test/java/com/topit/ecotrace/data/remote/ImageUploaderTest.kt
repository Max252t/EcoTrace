package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.remote.api.FilesApi
import com.topit.ecotrace.data.remote.api.UploadedFileDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ImageUploaderTest {

    private val filesApi: FilesApi = mockk()
    private val localImageSource: LocalImageSource = mockk()

    @Test
    fun upload_returnsUrlReturnedByServer() = runBlocking {
        givenLocalImage(byteArrayOf(1, 2, 3), "image/png")
        coEvery { filesApi.upload(any()) } returns UploadedFileDto("a.png", "/api/files/a.png")

        val result = uploader().upload(LOCAL_URI)

        assertEquals(ImageUploadResult.Success("/api/files/a.png"), result)
    }

    @Test
    fun upload_sendsFilePartWithContentTypeOfTheImage() = runBlocking {
        givenLocalImage(byteArrayOf(1, 2, 3), "image/png")
        val part = slot<MultipartBody.Part>()
        coEvery { filesApi.upload(capture(part)) } returns UploadedFileDto("a.png", "/api/files/a.png")

        uploader().upload(LOCAL_URI)

        assertEquals("image/png", part.captured.body.contentType().toString())
        assertEquals(3L, part.captured.body.contentLength())
        assertTrue(part.captured.headers?.get("Content-Disposition")?.contains("photo.png") == true)
    }

    @Test
    fun upload_reportsUnavailableWhenLocalFileCannotBeRead() = runBlocking {
        every { localImageSource.read(LOCAL_URI) } returns null

        assertEquals(ImageUploadResult.Unavailable, uploader().upload(LOCAL_URI))
        coVerify(exactly = 0) { filesApi.upload(any()) }
    }

    @Test
    fun upload_reportsUnavailableForEmptyFile() = runBlocking {
        every { localImageSource.read(LOCAL_URI) } returns ByteArray(0)

        assertEquals(ImageUploadResult.Unavailable, uploader().upload(LOCAL_URI))
    }

    @Test
    fun upload_reportsFailureWhenServerIsUnreachable() = runBlocking {
        givenLocalImage(byteArrayOf(1), "image/jpeg")
        coEvery { filesApi.upload(any()) } throws IOException("offline")

        assertEquals(ImageUploadResult.Failed, uploader().upload(LOCAL_URI))
    }

    @Test
    fun upload_reportsUnavailableWhenServerRejectsTheFile() = runBlocking {
        givenLocalImage(byteArrayOf(1), "application/pdf")
        coEvery { filesApi.upload(any()) } throws httpException(415)

        assertEquals(ImageUploadResult.Unavailable, uploader().upload(LOCAL_URI))
    }

    @Test
    fun upload_reportsFailureForServerError() = runBlocking {
        givenLocalImage(byteArrayOf(1), "image/jpeg")
        coEvery { filesApi.upload(any()) } throws httpException(500)

        assertEquals(ImageUploadResult.Failed, uploader().upload(LOCAL_URI))
    }

    @Test
    fun upload_fallsBackToJpegWhenContentTypeIsUnknown() = runBlocking {
        every { localImageSource.read(LOCAL_URI) } returns byteArrayOf(1)
        every { localImageSource.contentType(LOCAL_URI) } returns null
        val part = slot<MultipartBody.Part>()
        coEvery { filesApi.upload(capture(part)) } returns UploadedFileDto("a.jpg", "/api/files/a.jpg")

        uploader().upload(LOCAL_URI)

        assertEquals("image/jpeg", part.captured.body.contentType().toString())
    }

    private fun givenLocalImage(bytes: ByteArray, contentType: String) {
        every { localImageSource.read(LOCAL_URI) } returns bytes
        every { localImageSource.contentType(LOCAL_URI) } returns contentType
    }

    private fun httpException(code: Int) = HttpException(
        Response.error<Unit>(code, "".toResponseBody(null)),
    )

    private fun uploader() = ImageUploader(filesApi, localImageSource)

    private companion object {
        const val LOCAL_URI = "content://media/external/images/1"
    }
}
