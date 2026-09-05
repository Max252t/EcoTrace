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
        val bytes = localImageSource.readScaled(uri, MAX_DIMENSION, JPEG_QUALITY)
            ?.takeIf { it.isNotEmpty() }
            ?: return ImageUploadResult.Unavailable

        val part = MultipartBody.Part.createFormData(
            "file",
            "photo.jpg",
            bytes.toRequestBody(CONTENT_TYPE.toMediaTypeOrNull()),
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

    private companion object {
        const val CONTENT_TYPE = "image/jpeg"
        const val MAX_DIMENSION = 1920
        const val JPEG_QUALITY = 85
        val PERMANENT_ERRORS = setOf(400, 403, 404, 413, 415, 422)
    }
}
