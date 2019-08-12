package com.smart.hero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.smart.hero.Utils.Utils
import com.smart.hero.adapters.RecordAdapter
import com.smart.hero.data.model.Record
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_consulta.*

class ConsultaFragment: Fragment() {

    companion object {
        fun newInstance(): ConsultaFragment {
            return ConsultaFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_consulta, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val json: JsonObject = Parser.default().parse(StringBuilder(arguments!!.getString("EXTRA1"))) as JsonObject
        textNombre.text = "${json["nombre1"]} ${json["apellido1"]}"
        if (json.string("imagen")!!.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + json.string("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)

        val result = Klaxon().parseFromJsonArray<Record>(json.array<JsonObject>("historial")!!)
        val recordAdapter = RecordAdapter(activity!!.applicationContext, result!!)
        recyclerView.layoutManager = LinearLayoutManager(activity!!.applicationContext)
        recyclerView.adapter = recordAdapter
    }
}