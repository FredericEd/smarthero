package com.smart.hero.Utils

import com.google.android.gms.location.LocationResult
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_PROCESS_UPDATES == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val locations = result.locations
                    val locationResultHelper = LocationResultHelper(
                        context, locations
                    )
                    locationResultHelper.saveResults()
                    locationResultHelper.showNotification()
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(context))
                }
            }
        }
    }

    companion object {

        internal val ACTION_PROCESS_UPDATES = "com.smart.hero.Utils.LocationUpdatesBroadcastReceiver.action" + ".PROCESS_UPDATES"
        private val TAG = "LUBroadcastReceiver"
    }
}