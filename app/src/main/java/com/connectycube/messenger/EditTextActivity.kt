package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.MenuItem
import androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP
import androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE
import kotlinx.android.synthetic.main.activity_edit_text.*

const val EXTRA_TITLE: String = "extra_title"
const val EXTRA_DESCRIPTION: String = "extra_description"
const val EXTRA_EXIST_VALUE: String = "extra_exist_value"
const val EXTRA_MAX_LENGTH: String = "extra_max_length"
const val EXTRA_HINT: String = "extra_hint"
const val EXTRA_DATA: String = "extra_data"


class EditTextActivity : BaseChatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        initToolbar()
        initViews()
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions = DISPLAY_SHOW_TITLE or DISPLAY_HOME_AS_UP
        title = intent.getStringExtra(EXTRA_TITLE)
    }

    private fun initViews() {
        val maxLength: Int = intent.getIntExtra(EXTRA_MAX_LENGTH, 0)
        edit_text.hint = intent.getStringExtra(EXTRA_HINT)

        if (maxLength > 0) {
            edit_text.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }

        edit_text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return

                if (maxLength > 0) {
                    length_countdown_txt.text = (maxLength - s.length).toString()
                } else {
                    length_countdown_txt.text = s.length.toString()
                }
            }
        })

        edit_text.setText(intent.getStringExtra(EXTRA_EXIST_VALUE))
        description_txt.text = intent.getStringExtra(EXTRA_DESCRIPTION)
        cancel_btn.setOnClickListener { cancelEdit() }
        ok_btn.setOnClickListener { applyEdit(edit_text.text) }
    }

    private fun applyEdit(text: Editable) {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_DATA, text.toString())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun cancelEdit() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> cancelEdit()
        }

        return super.onOptionsItemSelected(item)
    }
}