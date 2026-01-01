package com.example.tvapp.model

import com.squareup.moshi.Json

// Represents the top-level JSON object from /api/devices/config/[deviceCode]
data class ScreenConfigResponse(
    val screen: Screen
)

// Represents the 'screen' object containing config and content
data class Screen(
    val id: String,
    val name: String,
    val orientation: String,
    val content: List<ContentItem>
)

// Represents an item in the 'content' array, matching the API documentation
data class ContentItem(
    val id: String,
    @Json(name = "duration_override") val durationOverride: Long?,
    val media: Media
) {
    // Helper function to map the new API structure to our internal PlaylistItem
    fun toPlaylistItem(): PlaylistItem {
        val isGoogleSlides = media.filePath.contains("docs.google.com/presentation")

        val type = when {
            isGoogleSlides -> "googleslides" // Specific type for Google Slides
            media.mimeType?.startsWith("video/") == true -> "video"
            media.mimeType?.startsWith("image/") == true -> "image"
            else -> "web" // Generic web content
        }

        val durationInMs = (durationOverride ?: media.duration)?.times(1000)

        return PlaylistItem(
            id = this.id,
            type = type,
            src = media.filePath,
            duration = durationInMs,
            fit = "cover",
            cachePolicy = "download"
        )
    }
}

// Represents the nested 'media' object inside a ContentItem
data class Media(
    val id: String,
    val name: String?,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "mime_type") val mimeType: String?,
    val duration: Long? // Duration in seconds from the media item itself
)
