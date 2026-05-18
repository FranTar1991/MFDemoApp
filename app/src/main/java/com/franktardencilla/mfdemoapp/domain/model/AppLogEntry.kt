package com.franktardencilla.mfdemoapp.domain.model

data class AppLogEntry(
    val timestampMillis: Long,
    val category: AppLogCategory,
    val message: String
)
