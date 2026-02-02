package com.example.tvapp.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.Executors

interface PlaybackListener {
    fun onPlaybackEnded()
    fun onPlaybackError(error: PlaybackException)
}

class PlayerController(context: Context) {

    private var player: ExoPlayer?
    private var listener: PlaybackListener?
    private val cache: SimpleCache
    private val downloadManager: DownloadManager

    init {
        val cacheDir = File(context.cacheDir, "exoplayer")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
        val databaseProvider = StandaloneDatabaseProvider(context)
        cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        val upstreamDataSourceFactory = DefaultDataSource.Factory(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        downloadManager = DownloadManager(
            context,
            databaseProvider,
            cache,
            upstreamDataSourceFactory,
            Executors.newSingleThreadExecutor()
        )

        listener = null

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    listener?.onPlaybackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                listener?.onPlaybackError(error)
            }
        })
    }

    fun setListener(listener: PlaybackListener?) {
        this.listener = listener
    }

    fun play(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    fun setAudioMuted(isMuted: Boolean) {
        player?.volume = if (isMuted) 0f else 1f
    }

    fun preload(url: String) {
        val downloadRequest = DownloadRequest.Builder(url, Uri.parse(url)).build()
        downloadManager.addDownload(downloadRequest)
    }

    fun attachPlayer(playerView: PlayerView) {
        playerView.player = player
    }

    fun detachPlayer(playerView: PlayerView) {
        playerView.player = null
    }

    fun releasePlayer() {
        downloadManager.release()
        player?.release()
        player = null
        cache.release()
    }
}
