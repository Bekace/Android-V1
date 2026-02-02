package com.example.tvapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.tvapp.R
import com.example.tvapp.cms.RetrofitClient
import com.example.tvapp.engine.PlaybackEngine
import com.example.tvapp.player.PlayerController
import com.example.tvapp.preferences.DevicePreferences

class PlaybackService : Service() {

    var playbackEngine: PlaybackEngine? = null
        private set
    lateinit var playerController: PlayerController
        private set

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getEngine(): PlaybackEngine? = playbackEngine
        fun getPlayerController(): PlayerController = playerController
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "PlaybackServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            playerController = PlayerController(this)
            val cmsService = RetrofitClient.instance
            val devicePreferences = DevicePreferences(this)
            playbackEngine = PlaybackEngine(this, cmsService, devicePreferences, playerController)

            try {
                createNotificationChannel()
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
            } catch (e: Throwable) {
                Log.e("PlaybackService", "Failed to start foreground service.", e)
            }

            playbackEngine?.start()

        } catch (t: Throwable) {
            Log.e("PlaybackService", "CATÁSTROPHIC: Error during service creation.", t)
            playbackEngine = null // Ensure engine is null on failure
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackEngine?.stop()
        if (::playerController.isInitialized) {
            playerController.releasePlayer()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Playback Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Hybrid Pro Player")
            .setContentText("Playback in progress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}