package com.example.tvapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tvapp.databinding.FragmentVideoBinding
import com.example.tvapp.player.PlayerController

class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private var playerController: PlayerController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This handles the case where the controller is set *before* the view is created.
        playerController?.attachPlayer(binding.playerView)
    }

    // This method is called by MainActivity to provide the shared PlayerController
    fun setPlayerController(controller: PlayerController) {
        this.playerController = controller
        // This handles the case where the view is created *before* the controller is set.
        // We check if the binding is not null to ensure the view is available.
        if (_binding != null) {
            playerController?.attachPlayer(binding.playerView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // It's crucial to detach the player to free up resources and prevent memory leaks.
        playerController?.detachPlayer(binding.playerView)
        _binding = null
    }



    companion object {
        fun newInstance(): VideoFragment {
            return VideoFragment()
        }
    }
}