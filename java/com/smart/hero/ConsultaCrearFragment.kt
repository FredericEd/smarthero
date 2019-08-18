package com.smart.hero

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.smart.hero.data.model.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_consulta_crear.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.HashMap

class ConsultaCrearFragment: Fragment() {

    private lateinit var prefs: SharedPreferences
    private val REQUEST_GET_SINGLE_FILE = 2735
    private lateinit var imgChosen: ImageView
    private var positionChosen = 0
    private val imagenes = arrayListOf<String?>(null, null, null, null)

    companion object {
        fun newInstance(): ConsultaCrearFragment {
            return ConsultaCrearFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_consulta_crear, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        img1.setOnClickListener{
            imgChosen = img1
            positionChosen = 0
            choosePicture()
        }
        img2.setOnClickListener{
            imgChosen = img2
            positionChosen = 1
            choosePicture()
        }
        img3.setOnClickListener{
            imgChosen = img3
            positionChosen = 2
            choosePicture()
        }
        img4.setOnClickListener{
            imgChosen = img4
            positionChosen = 3
            choosePicture()
        }
        btnFinalizar.setOnClickListener{
            attemptFinalizar()
        }
    }

    private fun choosePicture() {
        Dexter.withActivity(activity)
            .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
                        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
                        galleryIntent.type = "image/*"

                        val takePictureIntent =  Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        takePictureIntent.resolveActivity(activity!!.packageManager)

                        val chooser = Intent.createChooser(galleryIntent, "Some text here")
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
                        startActivityForResult(chooser, REQUEST_GET_SINGLE_FILE)
                    }
                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener{ Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()}
            .onSameThread()
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_GET_SINGLE_FILE) {
                if (data != null) {
                    val contentURI = data.data
                    if (contentURI != null) {
                        val afd: AssetFileDescriptor =
                            activity!!.contentResolver.openAssetFileDescriptor(contentURI, "r")!!
                        val fileSize: Long = afd.length
                        afd.close()
                        if (fileSize <= 5000000) {
                            val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, contentURI)
                            imgChosen.setImageBitmap(bitmap)

                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val b = baos.toByteArray()
                            val imgString = Base64.encodeToString(b, Base64.DEFAULT)
                            imagenes[positionChosen] = imgString
                        } else Toast.makeText(activity, R.string.error_heavy, Toast.LENGTH_LONG).show()
                    } else {
                        val imageBitmap = data.extras.get("data") as Bitmap
                        imgChosen.setImageBitmap(imageBitmap)

                        val baos = ByteArrayOutputStream()
                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val b = baos.toByteArray()
                        val imgString = Base64.encodeToString(b, Base64.DEFAULT)
                        imagenes[positionChosen] = imgString
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileSelectorActivity", "File select error", e)
        }
    }

    fun attemptFinalizar() {
        // Reset errors.
        editDetalles.error = null
        val detalles = editDetalles.text.toString()

        var cancel = false
        var focusView: View? = null
        if (detalles.isEmpty()) {
            editDetalles.error = getString(R.string.error_field_required)
            focusView = editDetalles
            cancel = true
        }
        val imagenesString = imagenes.filterNotNull().joinToString()
        if (imagenesString.isNullOrBlank()) {
            Toast.makeText(activity, R.string.error_image_least_required, Toast.LENGTH_LONG).show()
            return
        }
        if (!cancel) {
            crearRecord(detalles, imagenesString)
        } else focusView!!.requestFocus()
    }

    private fun crearRecord(detalle: String, imagenes: String) {
        if (!NetworkUtils.isConnected(activity!!.applicationContext)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            val URL = "${Utils.URL_SERVER}/denuncias"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity, json.string("message"), Toast.LENGTH_LONG).show()
                        activity!!.onBackPressed()
                    } catch (e: Exception) {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        error.printStackTrace()
                        Toast.makeText(
                            activity,
                            JSONObject(String(error.networkResponse.data)).getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }

                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["texto"] = detalle
                    parameters["imagenes"] = imagenes
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}