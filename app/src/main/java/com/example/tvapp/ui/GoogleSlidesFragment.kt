package com.example.tvapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.tvapp.databinding.FragmentGoogleSlidesBinding

class GoogleSlidesFragment : Fragment() {

    private var _binding: FragmentGoogleSlidesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoogleSlidesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString(ARG_WEB_URL) ?: return
        setupWebView()
        // Loading the URL directly as it was working before
        binding.webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.webView.visibility = View.INVISIBLE
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    binding.webView.visibility = View.VISIBLE
                }
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // Set a generic Android WebView User Agent String for TV optimization
                userAgentString = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.5359.128 Mobile Safari/537.36"
                setSupportZoom(false)
                builtInZoomControls = false
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            isFocusable = false
            isClickable = false
            setOnTouchListener { _, _ -> true }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // --- THE DEFINITIVE WEBVIEW MEMORY LEAK FIX ---
        binding.webView.apply {
            // Detach from the view hierarchy before destroying.
            (parent as? ViewGroup)?.removeView(this)
            stopLoading()
            // Clear history, cache, and other state.
            clearHistory()
            clearCache(true)
            clearFormData()
            // Finally, destroy the webview.
            destroy()
        }
        _binding = null
    }

    companion object {
        private const val ARG_WEB_URL = "web_url"
        private const val ARG_WEB_DURATION = "web_duration"

        fun newInstance(url: String, duration: Long): GoogleSlidesFragment {
            return GoogleSlidesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WEB_URL, url)
                    putLong(ARG_WEB_DURATION, duration)
                }
            }
        }
    }
}