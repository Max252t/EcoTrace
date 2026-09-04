package com.ecotrace.backend.data.storage

import java.io.File
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

    fun save(bytes: ByteArray, contentType: String?, originalFileName: String?): StoredFile {
        if (bytes.isEmpty()) return StoredFile.UnsupportedType
        if (bytes.size > maxFileSizeBytes) return StoredFile.TooLarge

        val extension = extensionOf(contentType, originalFileName) ?: return StoredFile.UnsupportedType
        val name = "${UUID.randomUUID()}.$extension"
        File(directory, name).writeBytes(bytes)

        return StoredFile.Success(name = name, url = urlFor(name))
    }

    fun delete(url: String?): Boolean {
        val name = url?.takeIf { it.startsWith("$ROUTE/") }?.removePrefix("$ROUTE/") ?: return false
        if (name.isEmpty() || !name.matches(SAFE_NAME)) return false
        return File(directory, name).delete()
    }

    private fun extensionOf(contentType: String?, originalFileName: String?): String? {
        val byContentType = EXTENSIONS_BY_CONTENT_TYPE[contentType?.substringBefore(';')?.trim()?.lowercase()]
        if (byContentType != null) return byContentType

        val byName = originalFileName?.substringAfterLast('.', "")?.lowercase()
        return byName?.takeIf { it in ALLOWED_EXTENSIONS }
    }

    companion object {
        const val ROUTE = "/api/files"

        private val EXTENSIONS_BY_CONTENT_TYPE = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        )

        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        private val SAFE_NAME = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$")

        fun urlFor(name: String): String = "$ROUTE/$name"
    }
}
