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
    fun save_writesJpegAndReturnsPublicUrl() {
        val bytes = jpeg(64)

        val stored = storage().save(bytes)

        assertTrue(stored is StoredFile.Success)
        assertTrue(stored.name.endsWith(".jpg"))
        assertEquals("/api/files/${stored.name}", stored.url)
        assertContentEquals(bytes, File(directory, stored.name).readBytes())
    }

    @Test
    fun save_detectsPngAndWebpBySignature() {
        val png = storage().save(png(32))
        val webp = storage().save(webp(32))

        assertTrue(png is StoredFile.Success)
        assertTrue(png.name.endsWith(".png"))
        assertTrue(webp is StoredFile.Success)
        assertTrue(webp.name.endsWith(".webp"))
    }

    @Test
    fun save_rejectsFileWithoutImageSignature() {
        val stored = storage().save("%PDF-1.7 not an image".toByteArray())

        assertEquals(StoredFile.UnsupportedType, stored)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun save_rejectsExecutableRenamedToJpg() {
        val stored = storage().save(byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0x00, 0x03, 0, 0, 0, 4, 0, 0, 0))

        assertEquals(StoredFile.UnsupportedType, stored)
    }

    @Test
    fun save_rejectsTruncatedFile() {
        assertEquals(StoredFile.UnsupportedType, storage().save(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }

    @Test
    fun save_rejectsEmptyFile() {
        assertEquals(StoredFile.UnsupportedType, storage().save(ByteArray(0)))
    }

    @Test
    fun save_stopsWritingWhenLimitIsExceeded() {
        val stored = storage(maxFileSizeBytes = 64).save(jpeg(512))

        assertEquals(StoredFile.TooLarge, stored)
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun save_generatesUniqueNameForEveryUpload() {
        val storage = storage()

        val first = storage.save(jpeg(16))
        val second = storage.save(jpeg(16))

        assertTrue(first is StoredFile.Success)
        assertTrue(second is StoredFile.Success)
        assertNotEquals(first.name, second.name)
        assertEquals(2, directory.listFiles()?.size)
    }

    @Test
    fun delete_removesStoredFileByItsUrl() {
        val storage = storage()
        val stored = storage.save(jpeg(16))
        assertTrue(stored is StoredFile.Success)

        assertTrue(storage.delete(stored.url))
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun delete_ignoresUrlsThatDoNotBelongToTheStorage() {
        val storage = storage()
        storage.save(jpeg(16))

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

    private companion object {
        fun jpeg(size: Int) = header(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), size)

        fun png(size: Int) = header(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            size,
        )

        fun webp(size: Int): ByteArray {
            val bytes = header(byteArrayOf(0x52, 0x49, 0x46, 0x46), size)
            byteArrayOf(0x57, 0x45, 0x42, 0x50).copyInto(bytes, 8)
            return bytes
        }

        private fun header(magic: ByteArray, size: Int): ByteArray {
            val bytes = ByteArray(maxOf(size, 12)) { 0x11 }
            magic.copyInto(bytes)
            return bytes
        }
    }
}
