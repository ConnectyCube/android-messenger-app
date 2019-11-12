package com.connectycube.messenger.utilities.image

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.connectycube.messenger.utilities.loadImagePreview

fun ImageView.loadPreview(url: String, onLoadingFinished: () -> Unit = {}) {
    val listener = object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?,
                                  model: Any?,
                                  target: com.bumptech.glide.request.target.Target<Drawable>?,
                                  isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }

        override fun onResourceReady(resource: Drawable?,
                                     model: Any?,
                                     target: com.bumptech.glide.request.target.Target<Drawable>?,
                                     dataSource: DataSource?,
                                     isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }
    }

    loadImagePreview(url, this, listener)
}