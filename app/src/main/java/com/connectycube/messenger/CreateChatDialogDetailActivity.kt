package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_create_chat_details.*

const val REQUEST_CREATE_DIALOG_DETAILS = 250
const val EXTRA_DIALOG_NAME = "dialog_name"
const val EXTRA_DIALOG_AVATAR = "dialog_avatar"

class CreateChatDialogDetailActivity : BaseChatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat_details)
        initToolbar()

    }

    private fun initToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> cancelResult()
            R.id.action_done -> setResult()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun cancelResult() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun setResult() {
        val name = chat_dialog_name_txt.text.toString()
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_DIALOG_NAME, name)
        resultIntent.putExtra(EXTRA_DIALOG_AVATAR, name)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}