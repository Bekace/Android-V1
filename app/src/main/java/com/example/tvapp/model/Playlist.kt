package com.example.tvapp.model

data class Playlist(
    val id: String,
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val id: String,
    val type: String,
    val src: String,
    val duration: Long? = null,
    val fit: String,
    val cachePolicy: String,
    val mute: Boolean? = null
)