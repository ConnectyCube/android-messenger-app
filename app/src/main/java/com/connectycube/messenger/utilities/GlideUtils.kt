package com.connectycube.messenger.utilities

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

fun loadChatDialogPhoto(isPrivate: Boolean, url: String?, imageView: ImageView, ctx: Context){
    val placeholder = if (isPrivate) R.drawable.ic_avatar_placeholder else R.drawable.ic_avatar_placeholder_group

    Glide.with(ctx)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .apply(RequestOptions.circleCropTransform())
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(imageView)
}