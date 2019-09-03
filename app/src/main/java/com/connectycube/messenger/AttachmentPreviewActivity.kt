package com.connectycube.messenger

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.connectycube.messenger.utilities.loadAttachImagePreview
import kotlinx.android.synthetic.main.activity_attachment_preview.*

const val EXTRA_URL = "attach_url"

class AttachmentPreviewActivity : BaseChatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attachment_preview)
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
    }

    private fun loadAttachment() {
        val url = intent.getStringExtra(EXTRA_URL)

        if (url.isNullOrEmpty()) {
            image_view.setImageResource(R.drawable.ic_image_black_24dp)
            return
        }
        loadAttachImagePreview(url, image_view, this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

}