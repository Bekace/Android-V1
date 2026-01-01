package com.example.tvapp.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.glance.visibility
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tvapp.R
import com.example.tvapp.databinding.ActivityMainBinding
import com.example.tvapp.engine.PlaybackEngine
import com.example.tvapp.engine.PlaybackState
import com.example.tvapp.player.PlayerController
import com.example.tvapp.service.PlaybackService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding
    private var playbackEngine: PlaybackEngine? = null
    private var playerController: PlayerController? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlaybackService.LocalBinder
            playbackEngine = binder.getEngine()
            playerController = binder.getPlayerController()
            isBound = true

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        playbackEngine?.state?.collectLatest { state ->
                            updateUiForState(state)
                        }
                    }
                    launch {
                        playbackEngine?.orientation?.collectLatest { orientation ->
                            applyOrientation(orientation)
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            playbackEngine = null
            playerController = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            startActivity(intent)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, PlaybackService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun applyOrientation(orientation: String) {
        requestedOrientation = when (orientation.lowercase()) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun updateUiForState(state: PlaybackState) {

        val statusMessage = when (state) {
            is PlaybackState.NeedsPairing -> "Pairing Code:\n${state.pairingCode}"
            is PlaybackState.Syncing -> "Syncing..."
            is PlaybackState.Preparing -> "Preparing ${state.item.type}..."
            is PlaybackState.Error -> "Error: ${state.message}"
            else -> null
        }

        if (statusMessage != null) {
            binding.statusTextview.text = statusMessage
            binding.statusTextview.visibility = View.VISIBLE
        } else {
            binding.statusTextview.visibility = View.GONE
        }

        // --- Fragment Swapping Logic ---
        val isPlayingOrPreparing = state is PlaybackState.Playing ||
                state is PlaybackState.PlayingWeb ||
                state is PlaybackState.Preparing

        if (isPlayingOrPreparing) {
            binding.fragmentContainer.visibility = View.VISIBLE
        } else {
            binding.fragmentContainer.visibility = View.GONE
        }

        if (state is PlaybackState.Playing || state is PlaybackState.PlayingWeb) {
            val newFragment = when (state) {
                is PlaybackState.Playing -> {
                    when (state.item.type) {
                        "video" -> VideoFragment.newInstance().apply {
                            playerController?.let { setPlayerController(it) }
                        }
                        "image" -> ImageFragment.newInstance(state.assetUri.toString())
                        else -> null
                    }
                }
                is PlaybackState.PlayingWeb -> {
                    val duration = state.item.duration ?: 60000L
                    when (state.item.type) {
                        "googleslides" -> GoogleSlidesFragment.newInstance(state.item.src, duration)
                        else -> WebFragment.newInstance(state.item.src, duration)
                    }
                }
                else -> null
            }
            newFragment?.let { swapFragment(it) }
        }

        // --- Overlay Style Logic ---
        val isBlocking = state is PlaybackState.NeedsPairing || state is PlaybackState.Error
        if (isBlocking) {
            binding.statusTextview.apply {
                gravity = Gravity.CENTER
                setBackgroundColor(if (state is PlaybackState.Error) Color.RED else Color.TRANSPARENT)
                textSize = if (state is PlaybackState.NeedsPairing) 48f else 20f
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    setMargins(0, 0, 0, 0)
                }
            }
        } else {
            binding.statusTextview.apply {
                gravity = Gravity.START
                background = getDrawable(R.drawable.status_background)
                textSize = 16f
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    setMargins(24, 24, 24, 24)
                }
            }
        }
    }

    private fun swapFragment(fragment: Fragment) {
        if (!isFinishing && !isDestroyed) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}