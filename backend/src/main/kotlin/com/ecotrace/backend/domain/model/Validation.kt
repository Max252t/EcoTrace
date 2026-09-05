package com.ecotrace.backend.domain.model

object Limits {
    const val EMAIL_MAX = 255
    const val DISPLAY_NAME_MAX = 128
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 72
    const val TITLE_MAX = 255
    const val DESCRIPTION_MAX = 4000
    const val PAGE_SIZE_DEFAULT = 100
    const val PAGE_SIZE_MAX = 500

    private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")

    fun isEmail(value: String): Boolean = value.length <= EMAIL_MAX && EMAIL_PATTERN.matches(value)

    fun isLatitude(value: Double): Boolean = value.isFinite() && value in -90.0..90.0

    fun isLongitude(value: Double): Boolean = value.isFinite() && value in -180.0..180.0
}
