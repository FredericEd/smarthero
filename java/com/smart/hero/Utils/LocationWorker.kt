package com.smart.hero.Utils

import android.content.Context
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.HashMap

class LocationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    lateinit var screenWakeLock: PowerManager.WakeLock

    override fun doWork(): Result {
        return try {
            if (!::screenWakeLock.isInitialized) {
                val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                screenWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "hero:location"
                )
            }
            screenWakeLock.acquire()
            saveRegistro()
            Log.e("SUCCESS", "LocationWorker ran")
            Result.SUCCESS
        } catch (e: Exception) {
            Log.e("ERROR", "Failure in doing work")
            Result.FAILURE
        }
    }

    private fun saveRegistro() {
        if (NetworkUtils.isConnected(applicationContext)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val queue = Volley.newRequestQueue(applicationContext)
            var URL = "${Utils.URL_SERVER}/bitacoras/${prefs.getString("id_bitacora", "")!!}/registros"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                try {
                    Log.e("respuesta", response)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    screenWakeLock?.let{
                        if (screenWakeLock.isHeld()) screenWakeLock.release()
                    }
                }
            }, Response.ErrorListener { error ->
                error.printStackTrace()
                screenWakeLock?.let{
                    if (screenWakeLock.isHeld()) screenWakeLock.release()
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
}