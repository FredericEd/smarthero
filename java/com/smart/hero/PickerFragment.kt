package com.smart.hero

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import kotlinx.android.synthetic.main.fragment_picker.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class PickerFragment: Fragment() {

    private lateinit var prefs: SharedPreferences
    private val REQUEST_GET_SINGLE_FILE = 2735
    private val REQUEST_IMAGE_CAPTURE = 1356

    companion object {
        fun newInstance(): PickerFragment {
            return PickerFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_picker, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        imgGallery.setOnClickListener{
            Dexter.withActivity(activity)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            openGallery()
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
        imgCamera.setOnClickListener{
            Dexter.withActivity(activity)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            dispatchTakePictureIntent()
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
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GET_SINGLE_FILE)
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_GET_SINGLE_FILE) {
                if (data != null) {
                    val contentURI = data.data
                    val afd: AssetFileDescriptor = activity!!.contentResolver.openAssetFileDescriptor(contentURI,"r")!!
                    val fileSize: Long = afd.length
                    afd.close()
                    if (fileSize <= 5000000) {
                        val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, contentURI)
                        //imageView.setImageBitmap(bitmap)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val b = baos.toByteArray()
                        val imgString = Base64.encodeToString(b, Base64.DEFAULT)
                        saveConsulta(imgString)
                    } else Toast.makeText(activity, R.string.error_heavy, Toast.LENGTH_LONG).show()
                }
            }
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null) {
                    val imageBitmap = data.extras.get("data") as Bitmap
                    //imageView.setImageBitmap(imageBitmap)
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val b = baos.toByteArray()
                    val imgString = Base64.encodeToString(b, Base64.DEFAULT)
                    saveConsulta(imgString)
                }
            }
        } catch (e: Exception) {
            Log.e("FileSelectorActivity", "File select error", e)
        }
    }

    private fun saveConsulta(imagen: String) {
        if (!NetworkUtils.isConnected(this@PickerFragment.context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/consultas/ejemplo"
            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val fragment = ConsultaPreviaFragment()
                        val bundle = Bundle()
                        bundle.putString("EXTRA1", response)
                        fragment.arguments = bundle
                        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        error.printStackTrace()
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
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
                    parameters["latitud"] = "45634"//prefs.getString("latitud", "")!!
                    parameters["longitud"] = "43545765"//prefs.getString("longitud", "")!!
                    parameters["imagen"] = imagen
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}