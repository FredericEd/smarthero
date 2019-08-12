package com.smart.hero.Utils

import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.smart.hero.R
import com.squareup.picasso.Picasso
import org.json.JSONArray
import java.lang.Exception

class InfoWindowPolice: GoogleMap.InfoWindowAdapter {

    private var context: Context
    private var data: JSONArray

    constructor(ctx: Context, array: JSONArray) {
        context = ctx
        data = array
    }
    override fun getInfoContents(p0: Marker?): View {
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_police, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        val textDireccion = view.findViewById<TextView>(R.id.textDireccion)
        val imgIcon = view.findViewById<ImageView>(R.id.imgIcon)

        val policia = data.getJSONObject(p0!!.tag as Int).getJSONObject("policia")
        textNombre.text = "${policia.getString("nombre1")}  ${policia.getString("apellido1")}"
        textDireccion.text = policia.getString("correo")
        return view
    }

    override fun getInfoWindow(p0: Marker?): View {
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_police, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        val textDireccion = view.findViewById<TextView>(R.id.textDireccion)
        val imgIcon = view.findViewById<ImageView>(R.id.imgIcon)

        val policia = data.getJSONObject(p0!!.tag as Int).getJSONObject("policia")
        textNombre.text = "${policia.getString("nombre1")}  ${policia.getString("apellido1")}"
        textDireccion.text = policia.getString("correo")
        if (!policia.getString("imagen").isNullOrBlank())
            Picasso.get().load(Utils.URL_MEDIA +policia.getString("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon, object: com.squareup.picasso.Callback {
                override fun onSuccess() {
                    if (!policia.has("loaded")) {
                        policia.put("loaded", "1")
                        p0.hideInfoWindow()
                        p0.showInfoWindow()
                    }
                }
                override fun onError(e: Exception?) {
                    // Nothing to do here
                }
            })
        return view
    }
}