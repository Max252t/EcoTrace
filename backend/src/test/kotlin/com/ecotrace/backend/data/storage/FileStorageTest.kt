package com.ecotrace.backend.data.storage

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileStorageTest {

    private val directory: File = Files.createTempDirectory("ecotrace-uploads").toFile()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun save_writesFileAndReturnsPublicUrl() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val stored = storage().save(bytes, "image/jpeg", "photo.jpg")

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.endsWith(".jpg"))
        assertEquals("/api/files/${stored.name}", stored.url)
        assertContentEquals(bytes, File(directory, stored.name).readBytes())
    }

    @Test
    fun save_takesExtensionFromContentType() {
        val stored = storage().save(byteArrayOf(1), "image/png", "photo")

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.endsWith(".png"))
    }

    @Test
    fun save_fallsBackToFileNameExtensionWhenContentTypeIsMissing() {
        val stored = storage().save(byteArrayOf(1), null, "photo.WEBP")

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.endsWith(".webp"))
    }

    @Test
    fun save_generatesUniqueNameForEveryUpload() {
        val storage = storage()

        val first = storage.save(byteArrayOf(1), "image/jpeg", "photo.jpg")
        val second = storage.save(byteArrayOf(2), "image/jpeg", "photo.jpg")

        assertTrue(first is StoredFile.Success)
        assertTrue(second is StoredFile.Success)
        assertNotEquals(first.name, second.name)
        assertEquals(2, directory.listFiles()?.size)
    }

    @Test
    fun save_rejectsUnsupportedType() {
        val stored = storage().save(byteArrayOf(1), "application/pdf", "document.pdf")

        assertEquals(StoredFile.UnsupportedType, stored)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun save_rejectsExecutableDisguisedByContentType() {
        val stored = storage().save(byteArrayOf(1), "text/plain", "payload.sh")

        assertEquals(StoredFile.UnsupportedType, stored)
    }

    @Test
    fun save_rejectsEmptyFile() {
        assertEquals(StoredFile.UnsupportedType, storage().save(ByteArray(0), "image/jpeg", "photo.jpg"))
    }

    @Test
    fun save_rejectsFileOverTheSizeLimit() {
        val stored = storage(maxFileSizeBytes = 8).save(ByteArray(9), "image/jpeg", "photo.jpg")

        assertEquals(StoredFile.TooLarge, stored)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun save_ignoresCharsetSuffixOfContentType() {
        val stored = storage().save(byteArrayOf(1), "image/jpeg; charset=binary", null)

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.endsWith(".jpg"))
    }

    @Test
    fun save_neverBuildsNameFromUserSuppliedPath() {
        val stored = storage().save(byteArrayOf(1), "image/png", "../../etc/passwd.png")

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.none { it == '/' || it == '\\' })
        assertEquals(File(directory, stored.name).canonicalFile.parentFile, directory.canonicalFile)
    }

    @Test
    fun delete_removesStoredFileByItsUrl() {
        val storage = storage()
        val stored = storage.save(byteArrayOf(1), "image/jpeg", "photo.jpg")
        assertTrue(stored is StoredFile.Success)

        assertTrue(storage.delete(stored.url))
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun delete_ignoresUrlsThatDoNotBelongToTheStorage() {
        val storage = storage()
        storage.save(byteArrayOf(1), "image/jpeg", "photo.jpg")

        assertFalse(storage.delete(null))
        assertFalse(storage.delete("https://cdn.example.com/a.jpg"))
        assertFalse(storage.delete("content://media/external/images/1"))
        assertEquals(1, directory.listFiles()?.size)
    }

    @Test
    fun delete_rejectsPathTraversal() {
        val outside = File(directory.parentFile, "secret.txt").apply { writeText("secret") }

        assertFalse(storage().delete("/api/files/../${outside.name}"))
        assertTrue(outside.exists())

        outside.delete()
    }

    @Test
    fun storage_createsMissingDirectory() {
        val nested = File(directory, "nested/uploads")

        FileStorage(nested, maxFileSizeBytes = 1024)

        assertTrue(nested.isDirectory)
    }

    private fun storage(maxFileSizeBytes: Long = 1024) =
        FileStorage(directory = directory, maxFileSizeBytes = maxFileSizeBytes)
}
