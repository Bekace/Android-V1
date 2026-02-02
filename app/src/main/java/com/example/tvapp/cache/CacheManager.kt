package com.example.tvapp.cache

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

sealed class CacheState {
    data object NotCached : CacheState()
    data class Downloading(val progress: Int) : CacheState()
    data class Ready(val localUri: Uri) : CacheState()
    data class Error(val message: String) : CacheState()
}

class CacheManager(private val context: Context) {

    private val cacheDir = context.cacheDir

    fun getAsset(url: String): Flow<CacheState> = flow {
        val fileName = urlToFileName(url)
        val file = File(cacheDir, fileName)

        if (file.exists() && file.length() > 0) {
            emit(CacheState.Ready(Uri.fromFile(file)))
            return@flow
        }

        emit(CacheState.NotCached)

        try {
            downloadFile(url, file, this)
            emit(CacheState.Ready(Uri.fromFile(file)))
        } catch (e: Exception) {
            Log.e("CacheManager", "Download failed for $url", e)
            file.delete()
            emit(CacheState.Error("Download failed: ${e.message}"))
        }
    }
    
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                // More aggressive cleanup: delete the directory and recreate it.
                if (cacheDir.exists()) {
                    val deleted = cacheDir.deleteRecursively()
                    Log.d("CacheManager", "Cache directory deleted recursively: $deleted")
                }
                cacheDir.mkdirs()
                Log.d("CacheManager", "Local cache cleared successfully.")
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to clear cache", e)
            }
        }
    }

    private suspend fun downloadFile(
        url: String,
        destinationFile: File,
        flowCollector: FlowCollector<CacheState>
    ) {
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection()
            connection.connect()

            val fileSize = connection.contentLength
            if (fileSize <= 0) {
                connection.getInputStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext
            }

            var downloadedBytes = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            connection.getInputStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = ((downloadedBytes * 100) / fileSize).toInt().coerceIn(0, 100)
                        val lastReportedProgress = (((downloadedBytes - bytesRead) * 100L) / fileSize).toInt()
                        if (progress == 0 || progress == 100 || (progress - lastReportedProgress) >= 5) {
                            flowCollector.emit(CacheState.Downloading(progress))
                        }
                    }
                }
            }
        }
    }

    private fun urlToFileName(url: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}