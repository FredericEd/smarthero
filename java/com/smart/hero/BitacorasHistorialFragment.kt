package com.smart.hero

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.smart.hero.Utils.NetworkUtils
import com.smart.hero.Utils.Utils
import com.smart.hero.adapters.BitacoraAdapter
import kotlinx.android.synthetic.main.fragment_bitacoras.*
import org.json.JSONException

class BitacorasHistorialFragment: Fragment(), BitacorasUIObserver{

    private lateinit var bitacoraAdapter: BitacoraAdapter
    private lateinit var prefs: SharedPreferences
    private var bitacorasList: MutableList<JsonObject> = mutableListOf()

    companion object {
        fun newInstance(): BitacorasListaFragment {
            return BitacorasListaFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_bitacoras, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)

        bitacoraAdapter = BitacoraAdapter(this, activity!!.applicationContext, bitacorasList!!, true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = bitacoraAdapter
        swipeRefreshLayout.setOnRefreshListener {
            if (!NetworkUtils.isConnected(context!!)) {
                Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            } else resetAndLoad()
        }
        textTitle.text = "Mis Bitácoras"
        bitacoraAdapter.notifyDataSetChanged()
        resetAndLoad()
    }

    private fun resetAndLoad(){
        bitacorasList.clear()
        progressView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        loadHistorial()
    }

    override fun onElementClicked(temp: JsonObject) {
        val fragment = BitacoraMapFragment()
        val bundle = Bundle()
        bundle.putString("tipo", "3")
        bundle.putString("EXTRA1", temp.int("id_bitacora").toString())
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    private fun loadHistorial(){
        if (NetworkUtils.isConnected(context!!)) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = object : StringRequest(Method.GET, "${Utils.URL_SERVER}/usuarios/bitacoras",
                Response.Listener<String> { response ->
                    if (isAdded) {
                        try {
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            swipeRefreshLayout.isRefreshing = false
                            val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                            val bitacoras = json.array<JsonObject>("bitacoras")
                            bitacoras!!.forEach{
                                bitacorasList.add(it)
                            }
                            bitacoraAdapter.notifyDataSetChanged()
                            textEmpty.visibility = if (bitacoras.size > 0) View.GONE else View.VISIBLE
                        } catch (e: JSONException) {
                            Toast.makeText(this@BitacorasHistorialFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }, Response.ErrorListener { error ->
                    if (isAdded) {
                        try {
                            error.printStackTrace()
                            if (bitacorasList.size == 0) {
                                textEmpty.visibility = View.VISIBLE
                                progressView.visibility = View.GONE
                                contentView.visibility = View.VISIBLE
                                swipeRefreshLayout.isRefreshing = false
                                swipeRefreshLayout.visibility = View.GONE
                            } else {
                                recyclerView.recycledViewPool.clear()
                                bitacoraAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@BitacorasHistorialFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
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
            textEmpty.visibility = View.VISIBLE
        }
    }
}