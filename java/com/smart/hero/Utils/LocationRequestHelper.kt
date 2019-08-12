package com.smart.hero.Utils

import android.content.Context
import android.preference.PreferenceManager


internal object LocationRequestHelper {

    val KEY_LOCATION_UPDATES_REQUESTED = "location-updates-requested"

    fun setRequesting(context: Context, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value)
            .apply()
    }

    fun getRequesting(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false)
    }
}