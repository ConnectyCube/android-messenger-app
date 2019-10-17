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
import com.connectycube.messenger.utilities.CircleToRectTransition
import com.connectycube.messenger.utilities.loadAttachImagePreview
import kotlinx.android.synthetic.main.activity_image_preview.*


const val EXTRA_IMAGE_URL = "image_url"
const val EXTRA_IMAGE_PREVIEW_TITLE = "activity_title"

fun startImagePreview(activity: AppCompatActivity, url: String, title: CharSequence?, fromTransitionView: View?){
    val intent = Intent(activity, ImagePreviewActivity::class.java)
    intent.putExtra(EXTRA_IMAGE_URL, url)
    intent.putExtra(EXTRA_IMAGE_PREVIEW_TITLE, title)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && fromTransitionView != null) {
        val options = ActivityOptions.makeSceneTransitionAnimation(activity, fromTransitionView, "img_full_screen")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.sharedElementEnterTransition = CircleToRectTransition()

            window.sharedElementReturnTransition = CircleToRectTransition()

            window.sharedElementExitTransition = CircleToRectTransition()
        }

        initToolBar()
        loadAttachment()
    }

    private fun initToolBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.dark_transparent
                )
            )
        )
        supportActionBar?.title = intent.getStringExtra(EXTRA_IMAGE_PREVIEW_TITLE)
    }

    private fun loadAttachment() {
        val url = intent.getStringExtra(EXTRA_IMAGE_URL)

        loadAttachImagePreview(url, image_view, this)
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