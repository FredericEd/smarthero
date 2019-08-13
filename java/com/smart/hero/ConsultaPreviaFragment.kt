package com.smart.hero

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.smart.hero.Utils.Utils
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_consulta_previa.*

class ConsultaPreviaFragment: Fragment() {

    companion object {
        fun newInstance(): ConsultaPreviaFragment {
            return ConsultaPreviaFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_consulta_previa, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.wtf("response", arguments!!.getString("EXTRA1"))
        val json: JsonObject = Parser.default().parse(StringBuilder(arguments!!.getString("EXTRA1"))) as JsonObject
        val datos: JsonObject = json.obj("consultas")!!.obj("especial")!!
        textNombre.text = "${datos["nombre1"]} ${datos["apellido1"]}"
        val alerta = datos.obj("tipo_alerta")!!
        textAlerta.text = alerta.string("nombre")
        textAlerta.setBackgroundColor(Color.parseColor("#${alerta["color"]}"))
        if (datos.string("imagen")!!.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + datos.string("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)
        btnRecord.setOnClickListener{
            val fragment = ConsultaFragment()
            val bundle = Bundle()
            bundle.putString("EXTRA1", datos.toJsonString())
            fragment.arguments = bundle
            fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
        }
    }
}