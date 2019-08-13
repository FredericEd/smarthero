package com.smart.hero

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.hero.Utils.InfoWindowPolice
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import kotlinx.android.synthetic.main.fragment_picker.contentView
import kotlinx.android.synthetic.main.fragment_picker.progressView
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLoc: LatLng
    private val DEFAULT_ZOOM = 13f
    private var policiasArray = JSONArray()

    private var mapFragment: SupportMapFragment? = null

    companion object {
        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_map, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        currentLoc = LatLng(prefs.getString("latitud", "")!!.toDouble(), prefs.getString("longitud", "")!!.toDouble())

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
        }
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment as Fragment).commit()
        mapFragment!!.getMapAsync(this)
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
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(currentLoc, DEFAULT_ZOOM)
                        )
                        loadPolicias()
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

        //mMap.addMarker(MarkerOptions().position(currentLoc).icon(BitmapDescriptorFactory.fromResource(R.drawable.policia)))
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
    }

    private fun setMarkersOnMap() {
        for (i in 0 until policiasArray.length()) {
            val marker = mMap.addMarker(
                MarkerOptions().position(
                    LatLng(policiasArray.getJSONObject(i).getString("latitud").toDouble(),
                        policiasArray.getJSONObject(i).getString("longitud").toDouble())
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.policia)))
            marker.tag = i
        }
        mMap.setInfoWindowAdapter(InfoWindowPolice(this@MapFragment.context!!, policiasArray))
        mMap.setOnInfoWindowClickListener {
            if (policiasArray.length() > (it.tag as Int)) {
                val single = policiasArray.getJSONObject(it.tag as Int).getJSONObject("policia")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=" + single.getString("telefono"))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    private fun loadPolicias(){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/usuarios/policias"
            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json = JSONObject(response.replace("ï»¿", ""))
                        policiasArray = json.getJSONArray("policia_location")
                        setMarkersOnMap()
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
                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = currentLoc.latitude.toString()
                    parameters["longitud"] = currentLoc.longitude.toString()
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}