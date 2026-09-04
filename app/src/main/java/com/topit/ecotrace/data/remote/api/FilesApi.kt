package com.topit.ecotrace.data.remote.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FilesApi {
    @Multipart
    @POST("api/files")
    suspend fun upload(@Part file: MultipartBody.Part): UploadedFileDto
}

data class UploadedFileDto(
    val name: String,
    val url: String,
)
