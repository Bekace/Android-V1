<<<<<<< HEAD
// Crash fix and comment added by Gemini.
// Cache logic removed by Gemini.
// Corrected polling logic by Gemini.
=======
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
package com.example.tvapp.engine

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.PlaybackException
<<<<<<< HEAD
import coil.imageLoader
import coil.request.ImageRequest
=======
import com.example.tvapp.cache.CacheManager
import com.example.tvapp.cache.CacheState
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
import com.example.tvapp.cms.CmsService
import com.example.tvapp.model.DeviceInfo
import com.example.tvapp.model.DeviceRegisterRequest
import com.example.tvapp.model.PlaylistItem
import com.example.tvapp.model.HeartbeatPayload
import com.example.tvapp.model.PlaybackEventPayload
import com.example.tvapp.player.PlaybackListener
import com.example.tvapp.player.PlayerController
import com.example.tvapp.preferences.DevicePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.Executors

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class NeedsPairing(val pairingCode: String) : PlaybackState()
    data object Syncing : PlaybackState()
    data class Preparing(val item: PlaylistItem) : PlaybackState()
<<<<<<< HEAD
=======
    data class Downloading(val item: PlaylistItem, val progress: Int) : PlaybackState()
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    data class Playing(val item: PlaylistItem, val assetUri: Uri) : PlaybackState()
    data class PlayingWeb(val item: PlaylistItem) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

private val PlaybackEngineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class PlaybackEngine(
    private val context: Context,
    private val cmsService: CmsService,
<<<<<<< HEAD
=======
    private val cacheManager: CacheManager,
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    private val devicePreferences: DevicePreferences,
    private val playerController: PlayerController
) : PlaybackListener {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _orientation = MutableStateFlow("landscape")
    val orientation: StateFlow<String> = _orientation.asStateFlow()

    private val engineScope = CoroutineScope(PlaybackEngineDispatcher + SupervisorJob())
    private var playbackJob: Job? = null
    private var heartbeatJob: Job? = null
    private var configPollJob: Job? = null
<<<<<<< HEAD
    private var mediaContinuation: CancellableContinuation<Unit>? = null

=======
    private var videoContinuation: CancellableContinuation<Unit>? = null
    
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    @Volatile
    private var playlistHasUpdate = false

    fun start() {
        if (playbackJob?.isActive == true) return
        MainScope().launch {
            playerController.setListener(this@PlaybackEngine)
        }
        playbackJob = engineScope.launch {
<<<<<<< HEAD
=======
            cacheManager.clearCache()
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
            runLifecycle()
        }
    }

    fun stop() {
        engineScope.cancel()
        MainScope().launch {
            playerController.setListener(null)
        }
<<<<<<< HEAD
        mediaContinuation?.cancel()
=======
        videoContinuation?.cancel()
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
        PlaybackEngineDispatcher.close()
    }

    private suspend fun CoroutineScope.runLifecycle() {
        while (isActive) {
            try {
                val deviceCode = getOrRegisterDevice()
                if (!checkAndPollUntilPaired(deviceCode)) {
                    delay(15_000)
                    continue
                }
                startHeartbeat(deviceCode)
                startConfigPolling(deviceCode)
                runContentLoop(deviceCode)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.e("PlaybackEngine", "Critical error in lifecycle", t)
                withContext(Dispatchers.Main) {
                    _state.value = PlaybackState.Error("A critical error occurred: ${t.message}. Retrying...")
                }
                delay(15_000)
            }
        }
    }

    private suspend fun CoroutineScope.getOrRegisterDevice(): String {
        val savedCode = devicePreferences.screenCode
        if (savedCode != null) return savedCode

        val newDeviceCode = generateDeviceCode()
        withContext(Dispatchers.Main) { _state.value = PlaybackState.Syncing }

        while (isActive) {
            try {
                val deviceInfo = DeviceInfo(
                    userAgent = "Android TV App / ${Build.MODEL}",
                    timestamp = getIsoTimestamp(),
                    url = "https://v0-xkreen-ai.vercel.app/player"
                )
                val request = DeviceRegisterRequest(newDeviceCode, deviceInfo)
                cmsService.registerDevice(request)
                devicePreferences.screenCode = newDeviceCode
                return newDeviceCode
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PlaybackEngine", "Failed to register device", e)
                withContext(Dispatchers.Main) {
                    _state.value = PlaybackState.Error("Failed to register with server. Retrying...")
                }
                delay(10_000)
            }
        }
        throw CancellationException("Scope inactive, cannot register device")
    }

    private suspend fun CoroutineScope.checkAndPollUntilPaired(deviceCode: String): Boolean {
        withContext(Dispatchers.Main) { _state.value = PlaybackState.Syncing }
        try {
            val initialStatus = cmsService.getDeviceStatus(deviceCode)
            if (initialStatus.device.isPaired) {
                return true
            }
        } catch (e: Exception) {
            if (e is CancellationException) return false
            val errorMsg = if (e is HttpException) "Server returned error ${e.code()}" else e.message
            Log.e("PlaybackEngine", "Failed to perform initial pairing check: $errorMsg", e)
            withContext(Dispatchers.Main) { _state.value = PlaybackState.Error("Failed to check pairing status ($errorMsg). Retrying...") }
            delay(5000)
            return false
        }

        withContext(Dispatchers.Main) { _state.value = PlaybackState.NeedsPairing(deviceCode) }
        while (isActive) {
            try {
                val status = cmsService.getDeviceStatus(deviceCode)
                if (status.device.isPaired) {
                    return true
                }
                delay(2000)
            } catch (e: Exception) {
                if (e is CancellationException) return false
                Log.w("PlaybackEngine", "Polling for pairing status failed", e)
                withContext(Dispatchers.Main) { _state.value = PlaybackState.NeedsPairing(deviceCode) }
                delay(5000)
            }
        }
        return false
    }

    private fun startHeartbeat(deviceCode: String) {
        heartbeatJob?.cancel()
        heartbeatJob = engineScope.launch {
            while (isActive) {
                try {
                    val currentItem = when (val currentState = _state.value) {
                        is PlaybackState.Playing -> currentState.item
                        is PlaybackState.PlayingWeb -> currentState.item
                        else -> null
                    }
                    val payload = HeartbeatPayload(
                        appVersion = "1.0.0",
                        status = "online",
                        currentItemId = currentItem?.id,
                        cacheUsageBytes = 0
                    )
                    cmsService.postHeartbeat(deviceCode, payload)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("PlaybackEngine", "Failed to send heartbeat", e)
                }
                delay(30_000)
            }
        }
    }
<<<<<<< HEAD

=======
    
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    private fun sendEvent(eventType: String, deviceCode: String, itemId: String, message: String? = null) {
        engineScope.launch {
            try {
                val payload = PlaybackEventPayload(eventType, itemId, message)
                cmsService.postEvent(deviceCode, payload)
            } catch (e: Exception) {
                Log.w("PlaybackEngine", "Failed to send event $eventType for item $itemId", e)
            }
        }
    }

    private suspend fun CoroutineScope.runContentLoop(deviceCode: String) {
<<<<<<< HEAD
        var playlistItems: List<PlaylistItem> = emptyList()

        while (isActive) {
            try {
                if (playlistHasUpdate || playlistItems.isEmpty()) {
                    withContext(Dispatchers.Main) { _state.value = PlaybackState.Syncing }
                    val screenConfig = cmsService.getScreenConfig(deviceCode)

                    // **FIX:** Create a stable signature of the content.
                    val contentSignature = screenConfig.screen.content.joinToString { it.id }
                    devicePreferences.configHash = contentSignature.hashCode()

                    withContext(Dispatchers.Main) { _orientation.value = screenConfig.screen.orientation }
                    playlistItems = screenConfig.screen.content.map { it.toPlaylistItem() }
                    playlistHasUpdate = false
                }
=======
        while (isActive) {
            try {
                // Force the UI into a clean state before fetching new content
                withContext(Dispatchers.Main) { _state.value = PlaybackState.Idle }
                
                withContext(Dispatchers.Main) { _state.value = PlaybackState.Syncing }
                val screenConfig = cmsService.getScreenConfig(deviceCode)
                withContext(Dispatchers.Main) { _orientation.value = screenConfig.screen.orientation }
                val playlistItems = screenConfig.screen.content.map { it.toPlaylistItem() }
                playlistHasUpdate = false
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed

                if (playlistItems.isEmpty()) {
                    withContext(Dispatchers.Main) { _state.value = PlaybackState.Error("No content assigned. Waiting...") }
                    delay(30_000)
                    continue
                }

                for (i in playlistItems.indices) {
                    if (playlistHasUpdate) {
                        Log.d("PlaybackEngine", "Playlist update detected, restarting content loop.")
<<<<<<< HEAD
                        break
                    }

                    val currentItem = playlistItems[i]
                    val nextItem = playlistItems.getOrNull(i + 1) ?: playlistItems.first()

                    preloadItem(nextItem)
=======
                        break 
                    }

                    val currentItem = playlistItems[i]
                    val nextItem = playlistItems.getOrNull(i + 1)

                    if (nextItem != null && (nextItem.type == "video" || nextItem.type == "image")) {
                        launch(Dispatchers.IO) {
                            cacheManager.getAsset(nextItem.src).collect()
                        }
                    }
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed

                    withContext(Dispatchers.Main) { _state.value = PlaybackState.Preparing(currentItem) }
                    sendEvent("item_started", deviceCode, currentItem.id)

                    when (currentItem.type) {
                        "video" -> handleVideoItem(currentItem)
                        "image" -> handleMediaItem(currentItem)
                        "web", "googleslides" -> handleWebItem(currentItem)
                        else -> Log.w("PlaybackEngine", "Unsupported content type: ${currentItem.type}")
                    }
                    sendEvent("item_ended", deviceCode, currentItem.id)
                    if (!isActive) break
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PlaybackEngine", "Error in content loop", e)
                val currentItem = (_state.value as? PlaybackState.Playing)?.item
                currentItem?.let { sendEvent("item_error", deviceCode, it.id, e.message) }

                withContext(Dispatchers.Main) {
                    val errorMsg = if (e is HttpException) "Server returned error ${e.code()}" else e.message
                    _state.value = PlaybackState.Error("Error loading content: $errorMsg. Retrying...")
                }
                delay(10_000)
            }
        }
    }

<<<<<<< HEAD
    private fun CoroutineScope.preloadItem(item: PlaylistItem) {
        launch(Dispatchers.IO) {
            try {
                when (item.type) {
                    "video" -> playerController.preload(item.src)
                    "image" -> {
                        val request = ImageRequest.Builder(context)
                            .data(item.src)
                            .build()
                        context.imageLoader.enqueue(request)
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaybackEngine", "Failed to preload item: ${item.src}", e)
            }
        }
    }

=======
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    private fun startConfigPolling(deviceCode: String) {
        configPollJob?.cancel()
        configPollJob = engineScope.launch(Dispatchers.IO) {
            while(isActive) {
<<<<<<< HEAD
                delay(30_000)
                try {
                    val newConfig = cmsService.getScreenConfig(deviceCode)

                    // **FIX:** Create a stable signature for comparison.
                    val newContentSignature = newConfig.screen.content.joinToString { it.id }
                    if (newContentSignature.hashCode() != devicePreferences.configHash) {
                        playlistHasUpdate = true
                        Log.d("PlaybackEngine", "Playlist update detected by poller.")
=======
                delay(30_000) 
                try {
                    val oldConfigHash = devicePreferences.screenCode?.hashCode()
                    val newConfig = cmsService.getScreenConfig(deviceCode)
                    if(newConfig.hashCode() != oldConfigHash) {
                       playlistHasUpdate = true
                       Log.d("PlaybackEngine", "Playlist update detected by poller.")
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("PlaybackEngine", "Config poll failed.", e)
                }
            }
        }
    }
<<<<<<< HEAD

    private suspend fun handleVideoItem(item: PlaylistItem) {
        withContext(Dispatchers.Main) {
            _state.value = PlaybackState.Playing(item, Uri.parse(item.src))
            suspendCancellableCoroutine { continuation ->
                mediaContinuation = continuation
                playerController.play(item.src)
=======
    
    private suspend fun processCacheFlow(item: PlaylistItem): CacheState.Ready? {
        var finalState: CacheState.Ready? = null
        cacheManager.getAsset(item.src).onEach { cacheState ->
            withContext(Dispatchers.Main) {
                when (cacheState) {
                    is CacheState.Ready -> finalState = cacheState
                    is CacheState.Downloading -> _state.value = PlaybackState.Downloading(item, cacheState.progress)
                    is CacheState.Error -> {
                        Log.e("PlaybackEngine", "Failed to cache asset for item ${item.id}: ${cacheState.message}")
                        sendEvent("asset_error", devicePreferences.screenCode ?: "unknown", item.id, cacheState.message)
                    }
                    CacheState.NotCached -> _state.value = PlaybackState.Preparing(item)
                }
            }
        }.first { it is CacheState.Ready || it is CacheState.Error } 
        return finalState
    }

    private suspend fun handleVideoItem(item: PlaylistItem) {
        val readyState = processCacheFlow(item) ?: return
        withContext(Dispatchers.Main) {
            _state.value = PlaybackState.Playing(item, readyState.localUri)
            suspendCancellableCoroutine { continuation ->
                videoContinuation = continuation
                playerController.play(readyState.localUri.toString())
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
            }
        }
    }

    private suspend fun handleMediaItem(item: PlaylistItem) {
<<<<<<< HEAD
        withContext(Dispatchers.Main) {
            _state.value = PlaybackState.Playing(item, Uri.parse(item.src))
        }
        waitForDuration(item.duration ?: 30000L)
=======
        val readyState = processCacheFlow(item) ?: return
        withContext(Dispatchers.Main) {
            _state.value = PlaybackState.Playing(item, readyState.localUri)
        }
        delay(item.duration ?: 30000L)
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    }

    private suspend fun handleWebItem(item: PlaylistItem) {
        withContext(Dispatchers.Main) { _state.value = PlaybackState.PlayingWeb(item) }
<<<<<<< HEAD
        waitForDuration(item.duration ?: 60000L)
    }

    private suspend fun waitForDuration(duration: Long) {
        suspendCancellableCoroutine { continuation ->
            mediaContinuation = continuation
            val waitJob = engineScope.launch(Dispatchers.Default) {
                try {
                    delay(duration)
                } finally {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
            continuation.invokeOnCancellation {
                waitJob.cancel()
            }
        }
=======
        delay(item.duration ?: 60000L)
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    }

    private fun generateDeviceCode() = (1..5).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    override fun onPlaybackEnded() {
<<<<<<< HEAD
        mediaContinuation?.resume(Unit)
        mediaContinuation = null
=======
        videoContinuation?.resume(Unit)
        videoContinuation = null
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    }

    override fun onPlaybackError(error: PlaybackException) {
        Log.e("PlayerController", "Playback Error", error)
        val currentItem = (_state.value as? PlaybackState.Playing)?.item
        currentItem?.let { sendEvent("playback_error", devicePreferences.screenCode ?: "unknown", it.id, error.message) }
<<<<<<< HEAD
        mediaContinuation?.resume(Unit)
        mediaContinuation = null
=======
        videoContinuation?.resume(Unit)
        videoContinuation = null
>>>>>>> 2ba9d17e9b76c55abb22feceae21672220ffc1ed
    }
}