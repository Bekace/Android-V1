package com.example.tvapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.tvapp.databinding.FragmentWebPairingBinding

class WebPairingFragment : Fragment() {

    private var _binding: FragmentWebPairingBinding? = null
    private val binding get() = _binding!!

    // Callback to MainActivity when pairing is complete
    var onPairingCompleteListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebPairingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        binding.webView.loadUrl("https://v0-xkreen-ai.vercel.app/player")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            webChromeClient = WebChromeClient()
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
                    // After the page is finished loading, inject the pairing code if available
                    arguments?.getString(ARG_PAIRING_CODE)?.let { code ->
                        showPairingCode(code)
                    }
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

            // Add JavaScript Interface for communication from web to native
            addJavascriptInterface(AndroidInterface(), "AndroidInterface")
            
            isFocusable = false
            isClickable = false
            setOnTouchListener { _, _ -> true }
        }
    }

    // Public method to inject the pairing code into the web page
    fun showPairingCode(code: String) {
        // Ensure this runs on the main thread and after the page is loaded
        if (_binding != null) {
            binding.webView.evaluateJavascript("window.displayPairingCode('$code');", null)
        } else {
            // If WebView isn't ready yet, store the code in arguments for onPageFinished to pick up
            arguments = Bundle().apply { putString(ARG_PAIRING_CODE, code) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView() 
        // WebView cleanup to prevent memory leaks
        binding.webView.apply {
            (parent as? ViewGroup)?.removeView(this)
            stopLoading()
            clearHistory()
            clearCache(true)
            clearFormData()
            destroy()
        }
        _binding = null
    }

    private inner class AndroidInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        fun onPairingComplete() {
            // Call the listener on the main thread
            Handler(Looper.getMainLooper()).post {
                onPairingCompleteListener?.invoke()
            }
        }
    }

    companion object {
        private const val ARG_PAIRING_CODE = "pairing_code"

        fun newInstance(pairingCode: String): WebPairingFragment {
            return WebPairingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PAIRING_CODE, pairingCode)
                }
            }
        }
    }
}