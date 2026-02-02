package com.example.tvapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tvapp.service.PlaybackService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}