package dev.jombi.nyants.ppg.dto

import kotlinx.serialization.Serializable

@Serializable
data class PapagoErrorResponse(
    val errorCode: String,
    val errorMessage: String,
)