package com.example.tvapp.player

import android.graphics.Color
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*

class WebController : DefaultLifecycleObserver {

    private var webView: WebView? = null
    private var fallbackView: View? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun attachWebView(webView: WebView, fallbackView: View) {
        this.webView = webView
        this.fallbackView = fallbackView
        setupWebView()
    }

    private fun setupWebView() {
        webView?.apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            setBackgroundColor(Color.TRANSPARENT)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    fallbackView?.visibility = View.VISIBLE
                    view?.visibility = View.INVISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    fallbackView?.visibility = View.GONE
                    view?.visibility = View.VISIBLE
                }
            }
        }
    }

    fun loadUrl(url: String, durationSeconds: Long, onFinished: () -> Unit) {
        timerJob?.cancel()
        webView?.loadUrl(url)
        timerJob = scope.launch {
            delay(durationSeconds * 1000)
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        webView?.onPause()
        timerJob?.cancel()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        webView?.onResume()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        timerJob?.cancel()
        webView?.destroy()
        webView = null
        fallbackView = null
        scope.cancel()
    }
}