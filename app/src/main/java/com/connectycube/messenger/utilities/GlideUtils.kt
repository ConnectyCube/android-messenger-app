package com.connectycube.messenger.utilities

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.connectycube.messenger.R

fun loadChatDialogPhoto(isPrivate: Boolean, url: String?, imageView: ImageView){
    val placeholder = if (isPrivate) R.drawable.ic_avatar_placeholder else R.drawable.ic_avatar_placeholder_group

    Glide.with(imageView)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .fallback(placeholder)
        .apply(RequestOptions.circleCropTransform())
        .into(imageView)
}