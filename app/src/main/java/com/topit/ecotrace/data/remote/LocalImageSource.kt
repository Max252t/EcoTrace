package com.topit.ecotrace.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface LocalImageSource {
    fun isLocal(uri: String): Boolean

    /** Returns the image downscaled to [maxDimension] and encoded as JPEG, or null if it cannot be read. */
    fun readScaled(uri: String, maxDimension: Int, quality: Int): ByteArray?
}

@Singleton
class ContentResolverImageSource @Inject constructor(
    private val context: Context,
) : LocalImageSource {

    override fun isLocal(uri: String): Boolean =
        LOCAL_SCHEMES.any { uri.startsWith(it, ignoreCase = true) }

    override fun readScaled(uri: String, maxDimension: Int, quality: Int): ByteArray? = runCatching {
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (largestSide <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(largestSide, maxDimension)
        }
        val bitmap = context.contentResolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scaled = scaleDown(bitmap, maxDimension)
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            output.toByteArray()
        }
    }.getOrNull()

    private fun sampleSizeFor(largestSide: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (largestSide / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / largestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        val LOCAL_SCHEMES = listOf("content://", "file://")
    }
}
