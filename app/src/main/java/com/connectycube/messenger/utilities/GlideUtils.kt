package com.connectycube.messenger.utilities

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
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