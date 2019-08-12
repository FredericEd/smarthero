package com.smart.hero

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.beust.klaxon.Klaxon
import com.smart.hero.Utils.Utils
import com.smart.hero.adapters.SectionsPagerAdapter
import com.smart.hero.data.model.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.textNombre
import kotlinx.android.synthetic.main.fragment_profile_data.*
import org.json.JSONObject

class ProfileFragment: Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var usuario: User

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_profile, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sectionsPagerAdapter = SectionsPagerAdapter(this@ProfileFragment.context!!, childFragmentManager!!)
        view_pager.adapter = sectionsPagerAdapter
        tabs.setupWithViewPager(view_pager)
    }

    override fun onResume() {
        super.onResume()
        prefs = PreferenceManager.getDefaultSharedPreferences(this@ProfileFragment.context)
        usuario = Klaxon().parse<User>(prefs.getString("usuario", ""))!!
        textNombre.text = "${usuario.nombre1} ${usuario.apellido1}"
        if (usuario.imagen.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + usuario.imagen).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)
    }
}