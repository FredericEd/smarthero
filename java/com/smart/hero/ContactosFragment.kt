package com.smart.hero

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.smart.hero.adapters.ContactAdapter
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.fragment_contactos.*
import kotlinx.android.synthetic.main.fragment_contactos.btnContacto
import kotlinx.android.synthetic.main.fragment_contactos.contentView
import kotlinx.android.synthetic.main.fragment_contactos.progressView
import org.json.JSONException
import org.json.JSONObject

class ContactosFragment: Fragment(), ContactUIObserver{

    private var PICK_CONTACT = 20155
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var prefs: SharedPreferences
    private var contactosList: MutableList<User> = mutableListOf()

    companion object {
        fun newInstance(): ContactosFragment {
            return ContactosFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_contactos, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getString("id_bitacora", "") != "") {
            val fragment = BitacoraMapFragment()
            val bundle = Bundle()
            bundle.putString("tipo", "1")
            fragment.arguments = bundle
            fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
            return
        }

        contactAdapter = ContactAdapter(this, activity!!.applicationContext, contactosList!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = contactAdapter
        swipeRefreshLayout.setOnRefreshListener {
            if (!NetworkUtils.isConnected(context!!)) {
                Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            } else resetAndLoad()
        }
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (NetworkUtils.isConnected(this@ContactosFragment.context!!)) {
                    val usuario = contactosList[viewHolder.adapterPosition]
                    contactosList.removeAt(viewHolder.adapterPosition)
                    contactAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                    onDeleteClicked(usuario)
                } else Toast.makeText(this@ContactosFragment.context, R.string.error_internet, Toast.LENGTH_LONG).show()
            }

            override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
                return false
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        contactAdapter.notifyDataSetChanged()

        btnContacto.setOnClickListener{
            Dexter.withActivity(activity)
                .withPermission(Manifest.permission.READ_CONTACTS)
                .withListener(object: PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        val i = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        startActivityForResult(i, PICK_CONTACT)
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
        btnSubmit.setOnClickListener{
            saveBitacora()
        }
        getDeviceLocation()
        resetAndLoad()
    }

    private fun resetAndLoad(){
        contactosList.clear()
        progressView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        loadContactos()
    }

    override fun onDeleteClicked(temp: User) {
        deleteContacto(temp.id_usuario)
    }

    private fun loadContactos(){
        if (NetworkUtils.isConnected(context!!)) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = object : StringRequest(Method.GET, "${Utils.URL_SERVER}/usuarios/contactos",
                Response.Listener<String> { response ->
                    if (isAdded) {
                        try {
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            swipeRefreshLayout.isRefreshing = false
                            Log.wtf("response", response)
                            val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                            val contactos = json.array<JsonObject>("contactos")
                            contactos!!.forEach{
                                try {
                                    val single = Klaxon().parseFromJsonObject<User>(it.obj("contacto")!!)
                                    contactosList.add(single!!)
                                } catch (e: java.lang.Exception) {
                                    e.printStackTrace()
                                }
                            }
                            contactAdapter.notifyDataSetChanged()
                        } catch (e: JSONException) {
                            Toast.makeText(this@ContactosFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }, Response.ErrorListener { error ->
                    if (isAdded) {
                        try {
                            if (contactosList.size == 0) {
                                error.printStackTrace()
                                progressView.visibility = View.GONE
                                contentView.visibility = View.VISIBLE
                                swipeRefreshLayout.isRefreshing = false

                                swipeRefreshLayout.visibility = View.GONE
                            } else {
                                recyclerView.recycledViewPool.clear()
                                contactAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@ContactosFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = java.util.HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        } else {
            Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
            progressView.visibility = View.GONE
            contentView.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
            swipeRefreshLayout.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_CONTACT) {
            if (data != null) {
                val contactUri: Uri = data.data
                val cursor = activity!!.contentResolver.query(contactUri, null, null, null, null);
                cursor.moveToFirst()
                val column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                Log.d("phone number", cursor.getString(column))
                searchUsuario(cursor.getString(column).replace("\\s".toRegex(), ""))
            }
        }
    }

    private fun searchUsuario(telefono: String){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/usuarios/search"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        val result = Klaxon().parseFromJsonObject<User>(json.obj("usuarios")!!)
                        contactosList.add(result!!)
                        contactAdapter.notifyDataSetChanged()
                        Toast.makeText(activity!!.applicationContext, json.string("message"), Toast.LENGTH_LONG).show()
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
                    parameters["telefono"] = telefono
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun deleteContacto(id_usuario: Int){
        val queue = Volley.newRequestQueue(context)
        val stringRequest = object : StringRequest(Method.DELETE, "${Utils.URL_SERVER}/contactos/$id_usuario",
            Response.Listener<String>{ response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity!!.applicationContext, json.string("message"), Toast.LENGTH_LONG).show()
                        resetAndLoad()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        Toast.makeText(this@ContactosFragment.context, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ContactosFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
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

    private fun saveBitacora() {
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/bitacoras"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity!!.applicationContext, json.string("message"), Toast.LENGTH_LONG).show()
                        prefs.edit().putString("id_bitacora", json.obj("bitacoras")!!.int("id_bitacora").toString()).apply()

                        (activity as MainActivity).requestLocationUpdates()
                        val fragment = BitacoraMapFragment()
                        val bundle = Bundle()
                        bundle.putString("tipo", "1")
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

    private fun getDeviceLocation() {
        try {
            val mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener{
                if (it.isSuccessful) {
                    val mLastKnownLocation = it.result
                    prefs.edit().putString("latitud", mLastKnownLocation!!.latitude.toString()).apply()
                    prefs.edit().putString("longitud", mLastKnownLocation.longitude.toString()).apply()
                } else {
                    Log.d("ERROR", "Current location is null. Using defaults.")
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }
}

interface ContactUIObserver{
    fun onDeleteClicked(temp: User)
}