package com.smart.hero.Utils

import com.google.android.gms.location.LocationResult
import android.content.Intent
import android.app.IntentService
import android.util.Log

class LocationUpdatesIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_PROCESS_UPDATES == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val locations = result.locations
                    val locationResultHelper = LocationResultHelper(
                        this, locations
                    )
                    locationResultHelper.saveResults()
                    locationResultHelper.showNotification()
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(this))
                }
            }
        }
    }

    companion object {

        internal val ACTION_PROCESS_UPDATES = "com.smart.hero.Utils.action" + ".PROCESS_UPDATES"
        private val TAG = LocationUpdatesIntentService::class.java.simpleName
    }
}