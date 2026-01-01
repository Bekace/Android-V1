package com.example.tvapp.model

import com.squareup.moshi.Json

// Data sent to the CMS for a heartbeat
data class HeartbeatPayload(
    @Json(name = "app_version") val appVersion: String,
    val status: String, // e.g., "online", "playing"
    @Json(name = "current_item_id") val currentItemId: String? = null,
    @Json(name = "cache_usage_bytes") val cacheUsageBytes: Long
)

// Data sent to the CMS for a specific playback event
data class PlaybackEventPayload(
    @Json(name = "event_type") val eventType: String, // e.g., "item_started", "item_error"
    @Json(name = "item_id") val itemId: String,
    val message: String? = null
)