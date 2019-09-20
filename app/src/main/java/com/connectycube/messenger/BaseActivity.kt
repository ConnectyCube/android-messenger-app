package com.connectycube.messenger

import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    fun hideProgress(progressbar: ProgressBar) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressbar.visibility = View.GONE
    }

    fun showProgress(progressbar: ProgressBar) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        progressbar.visibility = View.VISIBLE
    }

    fun showProgressValueIfNotNull(progressbar: ProgressBar, progress: Int?) {
        showProgress(progressbar)
        if (progress != null) {
            if (progressbar.isIndeterminate) progressbar.isIndeterminate = false
            progressbar.progress = progress
        } else if (!progressbar.isIndeterminate) progressbar.isIndeterminate = true
    }
}