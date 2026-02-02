package com.example.tvapp.preferences

import android.content.Context
import android.content.SharedPreferences

class DevicePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var screenCode: String?
        get() = prefs.getString(KEY_SCREEN_CODE, null)
        set(value) = prefs.edit().putString(KEY_SCREEN_CODE, value).apply()

    var configHash: Int
        get() = prefs.getInt(KEY_CONFIG_HASH, 0)
        set(value) = prefs.edit().putInt(KEY_CONFIG_HASH, value).apply()

    companion object {
        private const val PREFS_NAME = "device_prefs"
        private const val KEY_SCREEN_CODE = "screen_code"
        private const val KEY_CONFIG_HASH = "config_hash"
    }
}