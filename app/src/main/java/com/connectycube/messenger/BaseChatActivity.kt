package com.connectycube.messenger

import android.os.Bundle
import timber.log.Timber

abstract class BaseChatActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        ChatConnectionManager.getInstance().initWith(this)
    }
}