package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import com.connectycube.messenger.api.UserService
import com.connectycube.messenger.utilities.SharedPreferencesManager
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_settings.toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : BaseChatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initToolbar()

    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        back_btn.setOnClickListener { onBackPressed() }
        logout_btn.setOnClickListener {logout() }
    }

    private fun logout() {
        showProgress(progressbar)
        GlobalScope.launch(Dispatchers.Main) {
            UserService.instance.ultimateLogout(applicationContext)
            SharedPreferencesManager.getInstance(applicationContext).deleteCurrentUser()
            startLoginActivity()
            hideProgress(progressbar)

            finish()
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(intent)
    }
}