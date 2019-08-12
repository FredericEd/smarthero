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
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults()
                    // Show notification with the location data.
                    locationResultHelper.showNotification()
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(context))
                }
            }
        }
    }

    companion object {
        private val TAG = "LUBroadcastReceiver"

        internal val ACTION_PROCESS_UPDATES =
            "com.smart.hero.Utils.LocationUpdatesBroadcastReceiver.action" + ".PROCESS_UPDATES"
    }
}