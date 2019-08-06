package com.connectycube.messenger.utilities

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.connectycube.messenger.R
import com.connectycube.users.model.ConnectycubeUser

fun loadChatDialogPhoto(activityContext: Context, isPrivate: Boolean, url: String?, imageView: ImageView){
    val placeholder = if (isPrivate) R.drawable.ic_avatar_placeholder else R.drawable.ic_avatar_placeholder_group

    Glide.with(activityContext)
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

fun loadAttachImage(url: String?, imageView: ImageView, ctx: Context){
    val placeholder = R.drawable.ic_image_black_24dp
    val width = ctx.resources.getDimension(R.dimen.attach_image_width).toInt()
    val height = ctx.resources.getDimension(R.dimen.attach_image_height).toInt()
    val requestOptions = RequestOptions().transform(RoundedCorners(16))

    Glide.with(ctx)
        .load(url)
        .placeholder(placeholder)
        .override(width, height)
        .dontTransform()
        .apply(requestOptions)
        .error(placeholder)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(imageView)
}

fun loadUserAvatar(activityContext: Context, connectycubeUser: ConnectycubeUser, imageView: ImageView){
    val placeholder = R.drawable.ic_avatar_placeholder

    Glide.with(activityContext)
        .load(connectycubeUser.avatar)
        .placeholder(placeholder)
        .error(placeholder)
        .fallback(placeholder)
        .apply(RequestOptions.circleCropTransform())
        .into(imageView)
}