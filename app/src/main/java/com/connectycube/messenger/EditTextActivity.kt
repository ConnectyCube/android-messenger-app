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
import com.connectycube.messenger.databinding.ActivityEditTextBinding

const val EXTRA_TITLE: String = "extra_title"
const val EXTRA_DESCRIPTION: String = "extra_description"
const val EXTRA_EXIST_VALUE: String = "extra_exist_value"
const val EXTRA_MAX_LENGTH: String = "extra_max_length"
const val EXTRA_HINT: String = "extra_hint"
const val EXTRA_DATA: String = "extra_data"


class EditTextActivity : BaseChatActivity() {
    private lateinit var binding: ActivityEditTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        initViews()
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions = DISPLAY_SHOW_TITLE or DISPLAY_HOME_AS_UP
        title = intent.getStringExtra(EXTRA_TITLE)
    }

    private fun initViews() {
        val maxLength: Int = intent.getIntExtra(EXTRA_MAX_LENGTH, 0)
        binding.editText.hint = intent.getStringExtra(EXTRA_HINT)

        if (maxLength > 0) {
            binding.editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return

                if (maxLength > 0) {
                    binding.lengthCountdownTxt.text = (maxLength - s.length).toString()
                } else {
                    binding.lengthCountdownTxt.text = s.length.toString()
                }
            }
        })

        binding.editText.setText(intent.getStringExtra(EXTRA_EXIST_VALUE))
        binding.descriptionTxt.text = intent.getStringExtra(EXTRA_DESCRIPTION)
        binding.okBtn.setOnClickListener { applyEdit(binding.editText.text) }
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