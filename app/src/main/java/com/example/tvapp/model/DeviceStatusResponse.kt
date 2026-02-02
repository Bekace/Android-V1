package com.example.tvapp.model

import com.squareup.moshi.Json

data class DeviceStatusResponse(
    val device: DeviceStatus
)

data class DeviceStatus(
    val id: String,
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "is_paired") val isPaired: Boolean,
    @Json(name = "screen_id") val screenId: String?
)