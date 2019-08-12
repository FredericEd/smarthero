package com.smart.hero.Utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import com.smart.hero.MainActivity
import android.content.Intent
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.TaskStackBuilder
import androidx.core.app.NotificationCompat
import com.smart.hero.R
import java.text.DateFormat
import java.util.*


internal class LocationResultHelper(private val mContext: Context, private val mLocations: List<Location>) {
    private var mNotificationManager: NotificationManager? = null

    /**
     * Returns the title for reporting about a list of [Location] objects.
     */
    private val locationResultTitle: String
        get() {
            val numLocationsReported = mContext.getResources().getQuantityString(
                R.plurals.num_locations_reported, mLocations.size, mLocations.size
            )
            return numLocationsReported + ": " + DateFormat.getDateTimeInstance().format(Date())
        }

    private val locationResultText: String
        get() {
            if (mLocations.isEmpty()) {
                return mContext.getString(R.string.unknown_location)
            }
            val sb = StringBuilder()
            for (location in mLocations) {
                sb.append("(")
                sb.append(location.getLatitude())
                sb.append(", ")
                sb.append(location.getLongitude())
                sb.append(")")
                sb.append("\n")
            }
            return sb.toString()
        }

    /**
     * Get the notification mNotificationManager.
     *
     *
     * Utility method as this helper works with it a lot.
     *
     * @return The system service NotificationManager
     */
    private val notificationManager: NotificationManager
        get() {
            if (mNotificationManager == null) {
                mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return mNotificationManager!!
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(PRIMARY_CHANNEL, mContext.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.setLightColor(Color.GREEN)
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Saves location result as a string to [android.content.SharedPreferences].
     */
    fun saveResults() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs.edit().putString("latitud", mLocations[mLocations.lastIndex]!!.latitude.toString()).apply()
        prefs.edit().putString("longitud", mLocations[mLocations.lastIndex]!!.longitude.toString()).apply()
        PreferenceManager.getDefaultSharedPreferences(mContext)
            .edit()
            .putString(
                KEY_LOCATION_UPDATES_RESULT, locationResultTitle + "\n" +
                        locationResultText
            )
            .apply()
    }

    /**
     * Displays a notification with the location results.
     */
    fun showNotification() {
        val notificationIntent = Intent(mContext, MainActivity::class.java)

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(mContext)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(mContext, PRIMARY_CHANNEL)
            .setContentText(mContext.getString(R.string.persistent_notification))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(notificationPendingIntent)
        val note = notificationBuilder.build()
        note.flags = note.flags or Notification.FLAG_ONGOING_EVENT
        notificationManager.notify(0, note)
    }

    companion object {

        val KEY_LOCATION_UPDATES_RESULT = "location-update-result"

        private val PRIMARY_CHANNEL = "default"

        /**
         * Fetches location results from [android.content.SharedPreferences].
         */
        fun getSavedLocationResult(context: Context): String? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LOCATION_UPDATES_RESULT, "")
        }
    }
}