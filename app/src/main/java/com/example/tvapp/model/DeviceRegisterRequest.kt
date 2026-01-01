package com.example.tvapp.model

import com.squareup.moshi.Json

data class DeviceRegisterRequest(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "device_info") val deviceInfo: DeviceInfo
)

data class DeviceInfo(
    val userAgent: String,
    val timestamp: String,
    val url: String // Added this required field
)