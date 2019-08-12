package com.smart.hero.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smart.hero.ContactUIObserver
import com.smart.hero.R
import com.smart.hero.Utils.Utils
import com.smart.hero.data.model.User
import com.squareup.picasso.Picasso

class ContactAdapter(private val UIObserver: ContactUIObserver, private val mContext: Context, private val list: List<User>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ContactHolder(UIObserver, LayoutInflater.from(mContext).inflate(R.layout.item_contacto, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = genericHolder as ContactHolder
        holder.fillFields(list[position], position, mContext)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}

class ContactHolder(val UIObserver: ContactUIObserver, val view: View): RecyclerView.ViewHolder(view) {

    private val textNombre: TextView = view.findViewById(R.id.textNombre)
    private val textTelefono: TextView = view.findViewById(R.id.textTelefono)
    private val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
    private val imgDelete: ImageView = view.findViewById(R.id.imgDelete)

    fun fillFields(usuario: User, position: Int, mContext: Context){
        textNombre.text = "${usuario.nombre1} ${usuario.apellido1} ${usuario.apellido2}"
        textTelefono.text = usuario.telefono
        if (usuario.imagen.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + usuario.imagen).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon)
        imgDelete.setOnClickListener{
            UIObserver.onDeleteClicked(usuario)
        }
    }
}