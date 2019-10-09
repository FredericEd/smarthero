package com.smart.hero

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
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_consulta_previa.*
import org.json.JSONObject
import java.util.HashMap

class ConsultaPreviaFragment: Fragment() {

    companion object {
        fun newInstance(): ConsultaPreviaFragment {
            return ConsultaPreviaFragment()
        }
    }
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_consulta_previa, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        Log.wtf("response", arguments!!.getString("EXTRA1"))
        val json: JsonObject = Parser.default().parse(StringBuilder(arguments!!.getString("EXTRA1"))) as JsonObject
        val datos: JsonObject = json.obj("consultas")!!.obj("especial")!!
        textNombre.text = "${datos["nombre1"]} ${datos["apellido1"]}"
        val alerta = datos.obj("tipo_alerta")!!
        //textAlerta.text = alerta.string("nombre")
        //textAlerta.setBackgroundColor(Color.parseColor("#${alerta["color"]}"))
        if (datos.string("imagen")!!.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + datos.string("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)
        profilePicture.setBorderColor(Color.parseColor("#${alerta["color"]}"))
        btnRecord.setOnClickListener{
            val fragment = ConsultaFragment()
            val bundle = Bundle()
            bundle.putString("EXTRA1", arguments!!.getString("EXTRA1"))
            fragment.arguments = bundle
            fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
        }
        btnCrear.setOnClickListener {
            val fragment = ConsultaCrearFragment()
            fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
        }
        if (datos.array<JsonObject>("historial")!!.size > 0) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(getString(R.string.app_name)).setMessage(R.string.consulta_message_alarma)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                    dialog.cancel()
                    saveAlarma(json.obj("consultas")!!.int("id_consulta")!!.toString())
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun saveAlarma(id_consulta: String) {
        if (!NetworkUtils.isConnected(this@ConsultaPreviaFragment.context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            val queue = Volley.newRequestQueue(activity)
            val URL = "${Utils.URL_SERVER}/consultas/$id_consulta/alerta"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                try {
                    Log.wtf("response", response)
                    val json = JSONObject(response.replace("ï»¿", ""))
                    Toast.makeText(activity, json.getString("message"), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
            }, Response.ErrorListener { error ->
                try {
                    error.printStackTrace()
                    Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
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