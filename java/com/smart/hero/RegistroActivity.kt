package com.smart.hero

import android.Manifest
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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.facebook.accountkit.*
import com.facebook.accountkit.ui.AccountKitActivity
import com.facebook.accountkit.ui.AccountKitConfiguration
import com.facebook.accountkit.ui.LoginType
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.shagi.materialdatepicker.date.DatePickerFragmentDialog
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import kotlinx.android.synthetic.main.activity_registro.*
import org.json.JSONException
import org.json.JSONObject
import org.pcc.webviewOverlay.WebViewOverlay
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RegistroActivity : AppCompatActivity() {

    private var dbBirthday: String = ""
    private var imgString: String = ""
    private var phone: String = ""
    private lateinit var prefs: SharedPreferences
    private val REQUEST_IMAGE_CAPTURE = 1356
    private val ACCOUNTKIT_REQUEST_CODE = 8553

    private lateinit var webViewOverlay: WebViewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        webViewOverlay = WebViewOverlay(this)
    }

    private fun getAge(birthDate: Date, currentDate: Date): Int {
        val formatter = SimpleDateFormat("yyyyMMdd")
        val d1 = Integer.parseInt(formatter.format(birthDate))
        val d2 = Integer.parseInt(formatter.format(currentDate))
        return ((d2 - d1) / 10000)
    }

    fun onBirthday(view: View){
        val c = Calendar.getInstance()
        val mYear = if (dbBirthday.isEmpty()) c.get(Calendar.YEAR) else Integer.parseInt(dbBirthday.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        val mMonth = if (dbBirthday.isEmpty()) c.get(Calendar.MONTH) else (Integer.parseInt(dbBirthday.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) - 1)
        val mDay = if (dbBirthday.isEmpty()) c.get(Calendar.DAY_OF_MONTH) else Integer.parseInt(dbBirthday.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2])
        val dialog = DatePickerFragmentDialog.newInstance ({ _, year, monthOfYear, dayOfMonth ->
            val age  = getAge(GregorianCalendar(year, monthOfYear, dayOfMonth).time, Date())
            if (age >= 1) {
                var month = "" + (monthOfYear + 1)
                month = if (month.length == 1) "0$month" else month
                var day = "" + dayOfMonth
                day = if (day.length == 1) "0$day" else day
                dbBirthday = "$year-$month-$day"
                editDate.setText(dbBirthday)
            } else Toast.makeText(this@RegistroActivity, R.string.error_date_future, Toast.LENGTH_LONG).show()
        }, mYear, mMonth, mDay)
        dialog.show(supportFragmentManager, "tag")
    }

    fun goToTerms(view: View) {
        if (!NetworkUtils.isConnected(this@RegistroActivity)) {
            Toast.makeText(this@RegistroActivity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else webViewOverlay.loadWebViewOverlay(Utils.URL_TERMS, null)
    }

    fun attemptSave(view: View) {
        // Reset errors.
        editCedula.error = null
        editEmail.error = null
        editNombre1.error = null
        editNombre2.error = null
        editApellido1.error = null
        editApellido2.error = null
        editPassword.error = null
        editPassword2.error = null

        val cedula = editCedula.text.toString()
        val email = editEmail.text.toString()
        val nombre1 = editNombre1.text.toString()
        val nombre2 = editNombre2.text.toString()
        val apellido1 = editApellido1.text.toString()
        val apellido2 = editApellido2.text.toString()
        val password = editPassword.text.toString()
        val password2 = editPassword2.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!checkBox.isChecked) {
            Toast.makeText(this@RegistroActivity, R.string.error_terms, Toast.LENGTH_LONG).show()
            return
        }
        if (email.isEmpty()) {
            editEmail.error = getString(R.string.error_field_required)
            focusView = editEmail
            cancel = true
        } else if (!email.contains("@")) {
            editEmail.error = getString(R.string.error_invalid_email)
            focusView = editEmail
            cancel = true
        }
        if (cedula.isEmpty()) {
            editCedula.error = getString(R.string.error_field_required)
            focusView = editCedula
            cancel = true
        }
        if (nombre1.isEmpty()) {
            editNombre1.error = getString(R.string.error_field_required)
            focusView = editNombre1
            cancel = true
        }
        if (apellido1.isEmpty()) {
            editApellido1.error = getString(R.string.error_field_required)
            focusView = editApellido1
            cancel = true
        }
        if (apellido2.isEmpty()) {
            editApellido2.error = getString(R.string.error_field_required)
            focusView = editApellido2
            cancel = true
        }
        if (password.isEmpty()) {
            editPassword.error = getString(R.string.error_field_required)
            focusView = editPassword
            cancel = true
        } else if (password != password2) {
            editPassword2.setText("")
            editPassword2.error = getString(R.string.error_password_mismatch)
            focusView = editPassword2
            cancel = true
        }
        if (cancel) {
            focusView!!.requestFocus()
            return
        }
        if (dbBirthday.isEmpty()) {
            Toast.makeText(this@RegistroActivity, R.string.error_birthday_required, Toast.LENGTH_LONG).show()
            return
        }
        if (imgString.isEmpty()) {
            Toast.makeText(this@RegistroActivity, R.string.error_image_required, Toast.LENGTH_LONG).show()
            return
        }
        if (phone.isEmpty()) {
            Toast.makeText(this@RegistroActivity, R.string.error_phone_required, Toast.LENGTH_LONG).show()
            return
        }
        registerUser(cedula, email, nombre1, nombre2, apellido1, apellido2, password, editDate.text.toString(), phone, imgString)
    }

    private fun registerUser(cedula: String, correo: String, nombre1: String, nombre2: String, apellido1: String, apellido2: String, clave: String, fecha_nacimiento: String, telefono: String, imagen: String) {
        if (!NetworkUtils.isConnected(this@RegistroActivity)) {
            Toast.makeText(this@RegistroActivity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(this)
            val stringRequest = object : StringRequest(Request.Method.POST, "${Utils.URL_SERVER}/usuarios",
                    Response.Listener<String> { response ->
                        try {
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            Log.wtf("RESPONSE", response)
                            val json = JSONObject(response)
                            Toast.makeText(this@RegistroActivity, json.getString("message"), Toast.LENGTH_LONG).show()
                            finish()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(this@RegistroActivity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                        }
                    }, Response.ErrorListener { error ->
                        try {
                            error.printStackTrace()
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            val errorMessage = JSONObject(String(error.networkResponse.data)).getString("message")
                            Toast.makeText(this@RegistroActivity, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@RegistroActivity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                        }
                    }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["cedula"] = cedula
                    parameters["correo"] = correo
                    parameters["nombre1"] = nombre1
                    parameters["nombre2"] = nombre2
                    parameters["apellido1"] = apellido1
                    parameters["apellido2"] = apellido2
                    parameters["clave"] = clave
                    parameters["fecha_nacimiento"] = fecha_nacimiento
                    parameters["telefono"] = telefono
                    parameters["imagen"] = imagen
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    fun dispatchTakePictureIntent(view: View) {
        Dexter.withActivity(this@RegistroActivity)
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
                    Toast.makeText(this@RegistroActivity, R.string.error_permissions, Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    token!!.continuePermissionRequest()
                }
            }).
                withErrorListener{ Toast.makeText(this@RegistroActivity, R.string.error_permissions, Toast.LENGTH_SHORT).show()}
            .onSameThread()
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null) {
                    val imageBitmap = data.extras.get("data") as Bitmap
                    //imageView.setImageBitmap(imageBitmap)
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val b = baos.toByteArray()
                    imgString = Base64.encodeToString(b, Base64.DEFAULT)
                }
            }
            if (requestCode == ACCOUNTKIT_REQUEST_CODE){
                val loginResult: AccountKitLoginResult = data!!.getParcelableExtra(AccountKitLoginResult.RESULT_KEY)
                if(loginResult.getError() != null) {
                    Log.wtf("letsSee", "error: " + loginResult.getError().toString())
                } else if(loginResult.wasCancelled()) {
                    Log.wtf("letsSee", "cancelled")
                } else {
                    if(loginResult.getAccessToken() != null){
                        val accountid = loginResult.getAccessToken()!!.accountId
                        AccountKit.getCurrentAccount(object : AccountKitCallback<Account> {
                            override fun onSuccess(account: Account) {
                                val number = account.phoneNumber
                                val accountid = loginResult.getAccessToken()!!.accountId
                                phone = number!!.toString()
                                Log.d("number?.toString()", number?.toString())
                                editPhone.setText(phone)
                            }

                            override fun onError(error: AccountKitError) {}
                        })
                    }else{
                        Log.d("letsSee", "FAIL: " + loginResult.getAuthorizationCode()!!.substring(0,10))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileSelectorActivity", "File select error", e)
        }
    }

    fun phoneLogin(view: View) {
        val intent = Intent(this, AccountKitActivity::class.java)
        val builder = AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE, AccountKitActivity.ResponseType.TOKEN)

        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, builder.build())

        startActivityForResult(intent, ACCOUNTKIT_REQUEST_CODE)
    }
}
