package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.remote.api.FilesApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImageUploadResult {
    data class Success(val url: String) : ImageUploadResult
    data object Unavailable : ImageUploadResult
    data object Failed : ImageUploadResult
}

@Singleton
class ImageUploader @Inject constructor(
    private val filesApi: FilesApi,
    private val localImageSource: LocalImageSource,
) {
    fun isLocal(uri: String): Boolean = localImageSource.isLocal(uri)

    suspend fun upload(uri: String): ImageUploadResult {
        val bytes = localImageSource.read(uri)?.takeIf { it.isNotEmpty() }
            ?: return ImageUploadResult.Unavailable

        val contentType = localImageSource.contentType(uri) ?: DEFAULT_CONTENT_TYPE
        val part = MultipartBody.Part.createFormData(
            "file",
            fileNameFor(contentType),
            bytes.toRequestBody(contentType.toMediaTypeOrNull()),
        )

        return runCatching { filesApi.upload(part).url }.fold(
            onSuccess = { url ->
                if (url.isBlank()) ImageUploadResult.Failed else ImageUploadResult.Success(url)
            },
            onFailure = { error ->
                if (error is HttpException && error.code() in PERMANENT_ERRORS) {
                    ImageUploadResult.Unavailable
                } else {
                    ImageUploadResult.Failed
                }
            },
        )
    }

    private fun fileNameFor(contentType: String): String {
        val extension = when (contentType.substringBefore(';').trim().lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "photo.$extension"
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "image/jpeg"
        val PERMANENT_ERRORS = setOf(400, 403, 404, 413, 415, 422)
    }
}
