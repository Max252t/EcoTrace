package com.topit.ecotrace.data.remote

import android.content.Context
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

interface LocalImageSource {
    fun isLocal(uri: String): Boolean
    fun read(uri: String): ByteArray?
    fun contentType(uri: String): String?
}

@Singleton
class ContentResolverImageSource @Inject constructor(
    private val context: Context,
) : LocalImageSource {

    override fun isLocal(uri: String): Boolean =
        LOCAL_SCHEMES.any { uri.startsWith(it, ignoreCase = true) }

    override fun read(uri: String): ByteArray? = runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
    }.getOrNull()

    override fun contentType(uri: String): String? = runCatching {
        context.contentResolver.getType(Uri.parse(uri))
    }.getOrNull()

    private companion object {
        val LOCAL_SCHEMES = listOf("content://", "file://")
    }
}
