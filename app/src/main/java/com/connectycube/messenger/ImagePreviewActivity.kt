package com.connectycube.messenger

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.connectycube.messenger.databinding.ActivityImagePreviewBinding
import com.connectycube.messenger.utilities.image.loadPreview


const val EXTRA_IMAGE_URL = "image_url"
const val EXTRA_IMAGE_PREVIEW_TITLE = "activity_title"

fun startImagePreview(activity: AppCompatActivity,
                      url: String,
                      title: CharSequence?,
                      fromTransitionView: View
) {
    val statusBar = activity.findViewById<View>(android.R.id.statusBarBackground)
    val navigationBar = activity.findViewById<View>(android.R.id.navigationBarBackground)
    val toolBar = activity.findViewById<View>(R.id.toolbar)
    val inputLayout = activity.findViewById<View>(R.id.input_layout)

    val pairs = ArrayList<Pair<View, String>>()
    if (statusBar != null) {
        pairs.add(Pair(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
    }
    if (navigationBar != null) {
        pairs.add(Pair(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
    }
    if (toolBar != null) {
        pairs.add(Pair(toolBar, ViewCompat.getTransitionName(toolBar)!!))
    }
    if (inputLayout != null) {
        pairs.add(Pair(inputLayout, ViewCompat.getTransitionName(inputLayout)!!))
    }

    pairs.add(Pair(fromTransitionView, activity.getString(R.string.transition_name_image_view)))

    val intent = Intent(activity, ImagePreviewActivity::class.java)
    intent.putExtra(EXTRA_IMAGE_URL, url)
    intent.putExtra(EXTRA_IMAGE_PREVIEW_TITLE, title)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val options = ActivityOptions.makeSceneTransitionAnimation(
            activity,
            *pairs.toTypedArray()
        ).toBundle()

        activity.startActivity(intent, options)
    } else {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}

class ImagePreviewActivity : BaseActivity() {
    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportPostponeEnterTransition()

        initToolBar()
        loadAttachment()
    }

    private fun initToolBar() {
        val title = intent.getStringExtra(EXTRA_IMAGE_PREVIEW_TITLE)
        binding.imageTitle.text = title
        binding.backBtn.setOnClickListener { onBackPressed() }
        setSupportActionBar(binding.imageToolbar)
    }

    private fun loadAttachment() {
        val url = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (url.isNullOrEmpty()) {
            binding.imageView.setImageResource(R.drawable.ic_image_black_24dp)
            return
        }

        binding.imageView.loadPreview(url) {
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