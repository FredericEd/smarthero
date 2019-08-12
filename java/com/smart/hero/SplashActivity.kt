package com.smart.hero

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.beust.klaxon.Klaxon
import com.smart.hero.data.model.User

class SplashActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Handler().postDelayed({
            val i = if (prefs.getString("usuario", "") == "") {
                Intent(this@SplashActivity, LoginActivity::class.java)
            } else {
                Intent(this@SplashActivity, MainActivity::class.java)
            }
            startActivity(i)
            finish()
        }, 3000)
        if (prefs.getString("usuario", "") != "") {
            val usuario = Klaxon().parse<User>(prefs.getString("usuario", ""))!!
        }
    }
}