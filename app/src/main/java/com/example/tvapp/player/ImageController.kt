package com.example.tvapp.player

import android.widget.ImageView
import coil.load

class ImageController {

    fun showImage(imageView: ImageView, url: String) {
        imageView.load(url)
    }
}