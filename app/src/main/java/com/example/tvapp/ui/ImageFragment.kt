package com.example.tvapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil.load
import com.example.tvapp.R
import com.example.tvapp.databinding.FragmentImageBinding

class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        if (imageUrl == null) {
            showError("Image URL not provided.")
            return
        }

        try {
            binding.imageView.load(imageUrl) {
                crossfade(true)
                error(R.drawable.ic_launcher_background) // Show a background on error
                listener(onError = { _, throwable ->
                    showError("Failed to load image: ${throwable.message}")
                })
            }
        } catch (e: Exception) {
            showError("Error loading image: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Log.e("ImageFragment", message)
        binding.imageView.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): ImageFragment {
            return ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }
}