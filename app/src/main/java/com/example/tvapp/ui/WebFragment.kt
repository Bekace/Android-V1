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
import com.example.tvapp.databinding.FragmentWebBinding

class WebFragment : Fragment() {

    private var _binding: FragmentWebBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString(ARG_WEB_URL) ?: return
        setupWebView()
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
                // --- Features for general web content ---
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false // Allow autoplay for embedded media

                // --- Security and general settings ---
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
                setSupportZoom(false)
                builtInZoomControls = false
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            // Disable all user interaction
            isFocusable = false
            isClickable = false
            setOnTouchListener { _, _ -> true }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // --- Definitive WebView Cleanup to Prevent Memory Leaks ---
        binding.webView.apply {
            // 1. Stop any loading
            stopLoading()
            // 2. Clear history, cache, and other state
            clearHistory()
            clearCache(true)
            clearFormData()
            // 3. Detach from the view hierarchy before destroying
            (parent as? ViewGroup)?.removeView(this)
            // 4. Finally, destroy the webview
            destroy()
        }
        // --------------------------------------------------------
        _binding = null
    }

    companion object {
        private const val ARG_WEB_URL = "web_url"
        private const val ARG_WEB_DURATION = "web_duration"

        fun newInstance(url: String, duration: Long): WebFragment {
            return WebFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WEB_URL, url)
                    putLong(ARG_WEB_DURATION, duration)
                }
            }
        }
    }
}