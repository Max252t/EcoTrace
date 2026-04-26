package com.topit.ecotrace.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ReportsApi {
    @GET("api/reports")
    suspend fun getReports(): List<ReportResponseDto>

    @POST("api/reports")
    suspend fun createReport(@Body request: CreateReportRequestDto): ReportResponseDto

    @PATCH("api/reports/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body request: UpdateStatusRequestDto,
    ): ReportResponseDto

    @DELETE("api/reports/{id}")
    suspend fun deleteReport(@Path("id") id: String): Response<Unit>
}

data class CreateReportRequestDto(
    val title: String,
    val description: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
)

data class UpdateStatusRequestDto(
    val status: String,
)

data class ReportResponseDto(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val authorId: String,
    val createdAt: String,
    val updatedAt: String,
)
