package com.ecotrace.backend.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadedFileResponse(
    val name: String,
    val url: String,
)
