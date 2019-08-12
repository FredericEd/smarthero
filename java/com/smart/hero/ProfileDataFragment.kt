package com.smart.hero

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.smart.hero.Utils.InfoWindowPolice
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.smart.hero.adapters.RecordAdapter
import com.smart.hero.data.model.Record
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.fragment_profile_data.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ProfileDataFragment: Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var usuario: User

    companion object {
        fun newInstance(): ProfileDataFragment {
            return ProfileDataFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_profile_data, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        usuario = Klaxon().parse<User>(prefs.getString("usuario", ""))!!
        //usuario = User("2", "Freddy", "Eduardo", "Veloz", "Baez", "1992-08-20", "23452354", "freddy@gmail.com", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/HH_Polizeihauptmeister_MZ.jpg/220px-HH_Polizeihauptmeister_MZ.jpg")
        textNombre.text = "${usuario.nombre1} ${usuario.nombre2} ${usuario.apellido1} ${usuario.apellido2}"
        textCorreo.text = usuario.correo
        textNacimiento.text = usuario.fecha_nacimiento
        textPhone.text = usuario.telefono
        textEdad.text = getAge(usuario.fecha_nacimiento).toString() + " " + getString(R.string.profile_label_edad)
        btnRecord.setOnClickListener{
            loadRecord()
        }
        btnBack.setOnClickListener{
            card_view.visibility = View.VISIBLE
            layHistorial.visibility = View.GONE
        }
    }

    private fun getAge(fecha_nacimiento: String): Int {
        val mYear = Integer.parseInt(fecha_nacimiento.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        val mMonth = Integer.parseInt(fecha_nacimiento.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) - 1
        val mDay = Integer.parseInt(fecha_nacimiento.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2])
        val formatter = SimpleDateFormat("yyyyMMdd")
        val d1 = Integer.parseInt(formatter.format(GregorianCalendar(mYear, mMonth, mDay).time))
        val d2 = Integer.parseInt(formatter.format(Date()))
        return ((d2 - d1) / 10000)
    }

    private fun loadRecord(){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/usuarios/historial"
            val stringRequest = object : StringRequest(Request.Method.GET, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        val result = Klaxon().parseFromJsonArray<Record>(json.array<JsonObject>("historial")!!)
                        card_view.visibility = View.GONE
                        layHistorial.visibility = View.VISIBLE

                        val recordAdapter = RecordAdapter(activity!!.applicationContext, result!!)
                        recyclerView.layoutManager = LinearLayoutManager(activity!!.applicationContext)
                        recyclerView.adapter = recordAdapter
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
}