package com.example.tvapp.model

data class DeviceConfig(
    val orientation: String,
    val rotationDegrees: Int,
    val volume: Int,
    val telemetry: TelemetryConfig,
    val webview: WebViewConfig
)

data class TelemetryConfig(
    val heartbeatIntervalSeconds: Long
)

data class WebViewConfig(
    val allowedDomains: List<String>
)