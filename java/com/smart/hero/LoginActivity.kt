package com.smart.hero

import android.Manifest
import com.smart.hero.Utils.NetworkUtils
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
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
import com.facebook.accountkit.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.hero.Utils.Utils.Companion.URL_SERVER
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.contentView
import kotlinx.android.synthetic.main.activity_login.editEmail
import kotlinx.android.synthetic.main.activity_login.editPassword
import kotlinx.android.synthetic.main.activity_login.progressView
import kotlinx.android.synthetic.main.activity_registro.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val REQUEST_IMAGE_CAPTURE = 1356

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
            authUser(email, password, "")
        } else focusView!!.requestFocus()
    }

    private fun authUser(email: String, password: String, imagen: String) {
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
                    prefs.edit().putString("usuario", Klaxon().toJsonString(result)).apply()
                    prefs.edit().putString("api_key", json.obj("usuario")!!.string("api_key")).apply()
                    prefs.edit().putString("latitud", "-2.1925725").apply()
                    prefs.edit().putString("longitud", "-79.8803836").apply()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
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
                    if (imagen == "") {
                        parameters["correo"] = email
                        parameters["clave"] = password
                    } else parameters["imagen"] = imagen
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    fun dispatchTakePictureIntent(view: View) {
        Dexter.withActivity(this@LoginActivity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                        takePictureIntent.resolveActivity(applicationContext.packageManager)?.also {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                        }
                    }
                }
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@LoginActivity, R.string.error_permissions, Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    token!!.continuePermissionRequest()
                }
            }).
                withErrorListener{ Toast.makeText(this@LoginActivity, R.string.error_permissions, Toast.LENGTH_SHORT).show()}
            .onSameThread()
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null) {
                    val imageBitmap = data.extras.get("data") as Bitmap
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val b = baos.toByteArray()
                    val imgString = Base64.encodeToString(b, Base64.DEFAULT)
                    authUser("", "", imgString)
                }
            }
        } catch (e: Exception) {
            Log.e("FileSelectorActivity", "File select error", e)
        }
    }
}
