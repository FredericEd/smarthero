package com.smart.hero

import com.smart.hero.Utils.NetworkUtils
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.smart.hero.Utils.Utils.Companion.URL_SERVER
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    companion object {
        private var sharedInstance: LoginActivity? = null
        fun instance(): LoginActivity? {
            return sharedInstance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    fun createAccount(view: View) {
        startActivity(Intent(this, RegistroActivity::class.java))
    }

    fun attemptLogin(view: View) {
        // Reset errors.
        editEmail.error = null
        editPassword.error =  null
        val email = editEmail.text.toString()
        val password = editPassword.text.toString()

        var cancel = false
        var focusView: View? = null
        if (email.isEmpty()) {
            editEmail.error = getString(R.string.error_field_required)
            focusView = editEmail
            cancel = true
        } else if (!email.contains("@")) {
            editEmail.error = getString(R.string.error_invalid_email)
            focusView = editEmail
            cancel = true
        }
        if (password.isEmpty()) {
            editPassword.error = getString(R.string.error_field_required)
            focusView = editPassword
            cancel = true
        }
        if (!cancel) {
            authUser(email, password)
        } else focusView!!.requestFocus()
    }

    private fun authUser(email: String, password: String) {
        if (!NetworkUtils.isConnected(this@LoginActivity)) {
            Toast.makeText(this@LoginActivity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(this)
            val stringRequest = object : StringRequest(Method.POST, "${URL_SERVER}/usuarios/login", Response.Listener<String> { response ->
                try {
                    val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                    val result = Klaxon().parseFromJsonObject<User>(json.obj("usuario")!!)
                    /*val json = JSONObject(response)
                    val data = json.getJSONObject("usuario")
                    val user = User(data)*/
                    prefs.edit().putString("usuario", Klaxon().toJsonString(result)).apply()
                    prefs.edit().putString("api_key", json.obj("usuario")!!.string("api_key")).apply()
                    prefs.edit().putString("latitud", "-2.1925725").apply()
                    prefs.edit().putString("longitud", "-79.8803836").apply()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } catch (e: JSONException) {
                    progressView.visibility = View.GONE
                    contentView.visibility = View.VISIBLE
                    e.printStackTrace()
                    Toast.makeText(this@LoginActivity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
            }, Response.ErrorListener { error ->
                try {
                    progressView.visibility = View.GONE
                    contentView.visibility = View.VISIBLE
                    error.printStackTrace()
                    val errorMessage = JSONObject(String(error.networkResponse.data)).getString("message")
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
            }) {
                override fun getParams(): MutableMap<String, String> {
                    Log.i("email", email)
                    Log.i("password", password)

                    val parameters = HashMap<String, String>()
                    parameters["correo"] = email
                    parameters["clave"] = password
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}
