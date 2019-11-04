package com.connectycube.messenger

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.connectycube.messenger.utilities.image.loadPreview
import kotlinx.android.synthetic.main.activity_image_preview.*


const val EXTRA_IMAGE_URL = "image_url"
const val EXTRA_IMAGE_PREVIEW_TITLE = "activity_title"

fun startImagePreview(activity: AppCompatActivity,
                      url: String,
                      title: CharSequence?,
                      fromTransitionView: View?
) {
    val intent = Intent(activity, ImagePreviewActivity::class.java)
    intent.putExtra(EXTRA_IMAGE_URL, url)
    intent.putExtra(EXTRA_IMAGE_PREVIEW_TITLE, title)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && fromTransitionView != null) {
        val options = ActivityOptions.makeSceneTransitionAnimation(
            activity,
            fromTransitionView,
            activity.getString(R.string.transition_name)
        )
        activity.startActivity(intent, options.toBundle())
    } else {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}

class ImagePreviewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)
        supportPostponeEnterTransition()

        initToolBar()
        loadAttachment()
    }

    private fun initToolBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        this@ImagePreviewActivity,
                        R.color.dark_transparent
                    )
                )
            )
            title = intent.getStringExtra(EXTRA_IMAGE_PREVIEW_TITLE)
        }
    }

    private fun loadAttachment() {
        val url = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (url.isNullOrEmpty()) {
            image_view.setImageResource(R.drawable.ic_image_black_24dp)
            return
        }

        image_view.loadPreview(url) {
            supportStartPostponedEnterTransition()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}