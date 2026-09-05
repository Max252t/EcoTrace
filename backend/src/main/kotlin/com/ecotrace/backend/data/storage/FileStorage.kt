package com.ecotrace.backend.data.storage

import java.io.File
import java.io.InputStream
import java.util.UUID

sealed interface StoredFile {
    data class Success(val name: String, val url: String) : StoredFile
    data object TooLarge : StoredFile
    data object UnsupportedType : StoredFile
}

class FileStorage(
    val directory: File,
    private val maxFileSizeBytes: Long,
) {
    init {
        directory.mkdirs()
    }

    fun save(bytes: ByteArray): StoredFile = save(bytes.inputStream())

    fun save(input: InputStream): StoredFile {
        val header = ByteArray(HEADER_SIZE)
        val headerSize = input.readNBytes(header, 0, HEADER_SIZE)
        val extension = extensionOf(header, headerSize) ?: return StoredFile.UnsupportedType
        if (headerSize > maxFileSizeBytes) return StoredFile.TooLarge

        val name = "${UUID.randomUUID()}.$extension"
        val target = File(directory, name)
        var written = 0L

        target.outputStream().use { output ->
            output.write(header, 0, headerSize)
            written += headerSize

            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                written += read
                if (written > maxFileSizeBytes) {
                    output.close()
                    target.delete()
                    return StoredFile.TooLarge
                }
                output.write(buffer, 0, read)
            }
        }

        return StoredFile.Success(name = name, url = urlFor(name))
    }

    fun delete(url: String?): Boolean {
        val name = nameFromUrl(url) ?: return false
        return File(directory, name).delete()
    }

    private fun extensionOf(header: ByteArray, size: Int): String? {
        if (size < HEADER_SIZE) return null
        return when {
            header.startsWith(JPEG_MAGIC) -> "jpg"
            header.startsWith(PNG_MAGIC) -> "png"
            header.startsWith(RIFF_MAGIC) && header.copyOfRange(8, 12).contentEquals(WEBP_MAGIC) -> "webp"
            else -> null
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    companion object {
        const val ROUTE = "/api/files"

        private const val HEADER_SIZE = 12
        private const val BUFFER_SIZE = 8 * 1024

        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        private val RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        private val WEBP_MAGIC = byteArrayOf(0x57, 0x45, 0x42, 0x50)

        private val SAFE_NAME = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$")

        fun urlFor(name: String): String = "$ROUTE/$name"

        fun nameFromUrl(url: String?): String? {
            val name = url?.takeIf { it.startsWith("$ROUTE/") }?.removePrefix("$ROUTE/") ?: return null
            return name.takeIf { it.matches(SAFE_NAME) }
        }
    }
}
