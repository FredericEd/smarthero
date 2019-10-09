package com.smart.hero

import android.Manifest
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
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
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.fragment_bitacora_map.*
import kotlinx.android.synthetic.main.fragment_picker.contentView
import kotlinx.android.synthetic.main.fragment_picker.progressView
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class BitacoraMapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLoc: LatLng
    private val DEFAULT_ZOOM = 13f
    private lateinit var polyLine: Polyline
    private var id_bitacora = "0"

    private var mapFragment: SupportMapFragment? = null

    companion object {
        fun newInstance(): BitacoraMapFragment {
            return BitacoraMapFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_bitacora_map, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        id_bitacora = prefs.getString("id_bitacora", "0")!!
        if (arguments!!.getString("tipo") != "1") {
            id_bitacora = arguments!!.getString("EXTRA1")!!
            btnSubmit.visibility = View.INVISIBLE
        }
        currentLoc = LatLng(prefs.getString("latitud", "")!!.toDouble(), prefs.getString("longitud", "")!!.toDouble())

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
        }
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment as Fragment).commit()
        mapFragment!!.getMapAsync(this)
        btnActualizar.setOnClickListener {
            saveRegistro(id_bitacora)
        }
        btnSubmit.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.app_name)).setMessage(R.string.recorrido_message_finalizar)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                    dialog.cancel()
                    endBitacora()
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener{
                try {
                    if (it.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val mLastKnownLocation = it.result
                        prefs.edit().putString("latitud", mLastKnownLocation!!.latitude.toString()).apply()
                        prefs.edit().putString("longitud", mLastKnownLocation.longitude.toString()).apply()
                        currentLoc = LatLng(
                            mLastKnownLocation!!.latitude,
                            mLastKnownLocation.longitude
                        )
                    } else {
                        Log.d("ERROR", "Current location is null. Using defaults.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(activity, R.string.error_location, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc))
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
                    getDeviceLocation()
                }
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    token!!.continuePermissionRequest()
                }
            }).
                withErrorListener{ Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()}
            .onSameThread()
            .check()
        loadBitacora(id_bitacora)
    }

    private fun setRouteBetweenMarkers(registros: JsonArray<JsonObject>){
        if (::polyLine.isInitialized) {
            polyLine.remove()
        }
        var points: ArrayList<LatLng> = ArrayList()
        val lineOptions = PolylineOptions()
        registros.forEach{
            points.add(LatLng(it.string("latitud")!!.toDouble(), it.string("longitud")!!.toDouble()))
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(
            registros[0].string("latitud")!!.toDouble(),
            registros[0].string("longitud")!!.toDouble()), DEFAULT_ZOOM))
        mMap.addMarker(MarkerOptions().position(LatLng(
            registros[0].string("latitud")!!.toDouble(),
            registros[0].string("longitud")!!.toDouble())).title(getString(R.string.recorrido_label_inicio) + " (" + registros[0].string("fecha") + ")"))
        if (registros.size > 1) {
            mMap.addMarker(MarkerOptions().position(LatLng(
                registros[registros.lastIndex].string("latitud")!!.toDouble(),
                registros[registros.lastIndex].string("longitud")!!.toDouble())).title(getString(R.string.recorrido_label_final) + " (" + registros[registros.lastIndex].string("fecha") + ")"))
        }
        lineOptions.addAll(points)
        lineOptions.width(10.toFloat())
        lineOptions.color(R.color.colorAccent)

        if(lineOptions != null) {
            polyLine = mMap.addPolyline(lineOptions)
        } else {
            Log.wtf("onPostExecute","without Polylines drawn")
        }
    }

    private fun loadBitacora(id_bitacora: String){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            val URL = "${Utils.URL_SERVER}/bitacoras/$id_bitacora"
            val stringRequest = object : StringRequest(Method.GET, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        val contactos = json.obj("bitacoras")!!.array<JsonObject>("registros")!!
                        if (contactos.size > 0) setRouteBetweenMarkers(contactos)
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
                    Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
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
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun endBitacora() {
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/bitacoras/$id_bitacora/end"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity!!.applicationContext, json.string("message"), Toast.LENGTH_LONG).show()
                        prefs.edit().putString("id_bitacora", "").apply()
                        (activity as MainActivity).removeLocationUpdates()
                        activity!!.onBackPressed()
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
                        Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG
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
                    parameters["latitud"] = prefs.getString("latitud", "")!!
                    parameters["longitud"] = prefs.getString("longitud", "")!!
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun saveRegistro(id_bitacora: String) {
        if (NetworkUtils.isConnected(activity!!.applicationContext)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
            val queue = Volley.newRequestQueue(activity!!.applicationContext)
            var URL = "${Utils.URL_SERVER}/bitacoras/$id_bitacora/registros"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                try {
                    Log.e("respuesta", response)
                    loadBitacora(id_bitacora)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    /*screenWakeLock?.let{
                        if (screenWakeLock.isHeld()) screenWakeLock.release()
                    }*/
                }
            }, Response.ErrorListener { error ->
                error.printStackTrace()
                /*screenWakeLock?.let{
                    if (screenWakeLock.isHeld()) screenWakeLock.release()
                }*/
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }

                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = prefs.getString("latitud", "")!!
                    parameters["longitud"] = prefs.getString("longitud", "")!!
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}