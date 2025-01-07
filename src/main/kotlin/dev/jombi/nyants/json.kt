package dev.jombi.nyants

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    isLenient = true
    explicitNulls = true
    encodeDefaults = true
    allowSpecialFloatingPointValues = true
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = true
}