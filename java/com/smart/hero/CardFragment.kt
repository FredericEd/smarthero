package com.smart.hero

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.smart.hero.data.model.User
import kotlinx.android.synthetic.main.fragment_profile_data.*
import java.text.SimpleDateFormat
import java.util.*

class CardFragment: Fragment() {

    private lateinit var prefs: SharedPreferences

    companion object {
        fun newInstance(): CardFragment {
            return CardFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_card, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
    }
}