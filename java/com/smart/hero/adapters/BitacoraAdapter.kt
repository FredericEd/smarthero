package com.smart.hero.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.smart.hero.BitacorasUIObserver
import com.smart.hero.R
import com.smart.hero.Utils.Utils
import com.smart.hero.data.model.User
import com.squareup.picasso.Picasso
import java.lang.Exception

class BitacoraAdapter(private val UIObserver: BitacorasUIObserver, private val mContext: Context, private val list: List<JsonObject>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BitacoraHolder(UIObserver, LayoutInflater.from(mContext).inflate(R.layout.item_bitacora, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = genericHolder as BitacoraHolder
        holder.fillFields(list[position], position, mContext)
    }

    override fun getItemId(position: Int): Long {
        try {
            return list[position].int("id_bitacora")!!.toLong()
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return 0
    }
}

class BitacoraHolder(val UIObserver: BitacorasUIObserver, val view: View): RecyclerView.ViewHolder(view) {

    private val textNombre: TextView = view.findViewById(R.id.textNombre)
    private val textFecha: TextView = view.findViewById(R.id.textFecha)
    private val textEstado: TextView = view.findViewById(R.id.textEstado)
    private val imgIcon: ImageView = view.findViewById(R.id.imgIcon)

    fun fillFields(bitacora: JsonObject, position: Int, mContext: Context){
        val usuario = Klaxon().parseFromJsonObject<User>(bitacora.obj("usuario")!!)!!
        textNombre.text = "${usuario.nombre1} ${usuario.apellido1} ${usuario.apellido2}"
        textFecha.text = bitacora.string("fecha_inicio")
        textEstado.text = if (bitacora.string("fecha_fin") == "") "En curso" else "Finalizado"
        if (usuario.imagen.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + usuario.imagen).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon)
        view.setOnClickListener{
            UIObserver.onElementClicked(bitacora)
        }
    }
}