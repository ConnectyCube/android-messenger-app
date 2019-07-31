package com.connectycube.messenger

import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.activity.ComponentActivity

abstract class BaseChatActivity : ComponentActivity()  {

    fun hideProgress(progressbar: ProgressBar) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressbar.visibility = View.GONE
    }

    fun showProgress(progressbar: ProgressBar) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        progressbar.visibility = View.VISIBLE;
    }
}