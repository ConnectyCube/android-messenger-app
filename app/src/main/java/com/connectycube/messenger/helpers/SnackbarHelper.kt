package com.connectycube.messenger.helpers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import com.connectycube.messenger.R
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

fun showSnackbar(activity: Activity, @StringRes res: Int, @BaseTransientBottomBar.Duration duration: Int) {
    val snack = Snackbar.make(
        activity.findViewById(android.R.id.content),
        activity.getString(res),
        duration
    )
    snack.config(activity)
    snack.show()
}

private fun Snackbar.config(context: Context) {
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12)
    this.view.layoutParams = params

    this.view.background = AppCompatResources.getDrawable(context, R.drawable.snackbar_bg)

    ViewCompat.setElevation(this.view, 6f)
    setAction(R.string.snackbar_dismiss) { dismiss() }
}
