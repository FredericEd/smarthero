package com.smart.hero

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.Klaxon
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.smart.hero.Utils.*
import com.smart.hero.Utils.LocationRequestHelper
import com.smart.hero.data.model.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_picker.*
import org.json.JSONObject
import java.util.HashMap

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private lateinit var navView: NavigationView
    private lateinit var prefs: SharedPreferences
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mLocationRequest: LocationRequest

    private val UPDATE_INTERVAL = (10 * 1000).toLong()
    private val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2
    private val MAX_WAIT_TIME = UPDATE_INTERVAL * 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        navView.setNavigationItemSelectedListener(this)
        navView.menu.getItem(0).isChecked = true
        onNavigationItemSelected(navView.menu.getItem(0))
        setBottomBar()
        buildGoogleApiClient()
    }

    override fun onConnected(@Nullable bundle: Bundle?) {
        Log.wtf(MainActivity::class.java.simpleName, "GoogleApiClient connected")
    }

    fun requestLocationUpdates() {
        try {
            Log.i(MainActivity::class.java.simpleName, "Starting location updates")
            LocationRequestHelper.setRequesting(this, true)
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, getPendingIntent())
        } catch (e: SecurityException) {
            LocationRequestHelper.setRequesting(this, false)
            e.printStackTrace()
        }
    }

    fun removeLocationUpdates() {
        Log.i(MainActivity::class.java.simpleName, "Ending location updates")
        LocationRequestHelper.setRequesting(this, false);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getPendingIntent())
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancelAll()
    }

    override fun onConnectionSuspended(i: Int) {
        Log.wtf(MainActivity::class.java.simpleName, "Connection suspended")
    }

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        Log.wtf(MainActivity::class.java.simpleName, "Exception while connecting to Google Play services")
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_INTERVAL

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.maxWaitTime = MAX_WAIT_TIME
    }

    private fun getPendingIntent(): PendingIntent {
        val intent: Intent = Intent(this@MainActivity, LocationUpdatesBroadcastReceiver::class.java)
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun setBottomBar() {
        menu_map.setOnClickListener{
            navView.menu.getItem(0).isChecked = true
            onNavigationItemSelected(navView.menu.getItem(0))
        }
        menu_detect.setOnClickListener{
            navView.menu.getItem(1).isChecked = true
            onNavigationItemSelected(navView.menu.getItem(1))
        }
        menu_profile.setOnClickListener{
            navView.menu.getItem(2).isChecked = true
            onNavigationItemSelected(navView.menu.getItem(2))
        }
        menu_recorrido.setOnClickListener{
            navView.menu.getItem(3).isChecked = true
            onNavigationItemSelected(navView.menu.getItem(3))
        }
    }

    override fun onResume() {
        super.onResume()
        val usuario = Klaxon().parse<User>(prefs.getString("usuario", ""))!!
        val headerView = navView.getHeaderView(0)
        val textNombreDrawer = headerView.findViewById(R.id.textNombreDrawer) as TextView
        val profilePicture = headerView.findViewById(R.id.profilePicture) as CircleImageView
        textNombreDrawer.text = "${usuario.nombre1} ${usuario.apellido1}"
        if (usuario.imagen.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + usuario.imagen).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val anonymousFragment = supportFragmentManager.findFragmentById(R.id.frame_container)
            if (anonymousFragment is MapFragment) {
                super.onBackPressed()
            } else {
                val intent = intent
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_alarma -> {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(getString(R.string.app_name)).setMessage(R.string.home_message_alarma)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                        dialog.cancel()
                        saveAlarma()
                    }
                    .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
                val alert = builder.create()
                alert.show()
                true
            }
            R.id.action_logout -> {
                prefs.edit().putString("usuario", "").apply()
                prefs.edit().putString("api_key", "").apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_map -> displayView(0)
            R.id.nav_detect -> displayView(1)
            R.id.nav_profile -> displayView(2)
            R.id.nav_recorrido -> displayView(3)
            R.id.nav_shared -> displayView(4)
            /*R.id.nav_settings -> displayView(5)*/
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun displayView(position: Int) {
        menu_map.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_detect.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_profile.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_recorrido.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        var fragment: Fragment? = null
        val bundl = Bundle()
        when (position) {
            0 -> {
                menu_map.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = MapFragment()
            }
            1 -> {
                menu_detect.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = PickerFragment()
            }
            2 -> {
                menu_profile.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = ProfileFragment()
            }
            3 -> {
                menu_recorrido.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = ContactosFragment()
            }
            4 -> fragment = BitacorasListaFragment()
        }
        if (fragment != null) {
            fragment!!.arguments = bundl
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
        } else Log.e("MainActivity", "Error in creating fragment")
    }


    private fun saveAlarma() {
        if (!NetworkUtils.isConnected(applicationContext)) {
            Toast.makeText(applicationContext, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            val queue = Volley.newRequestQueue(applicationContext)
            val URL = "${Utils.URL_SERVER}/alarmas"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                try {
                    val json = JSONObject(response.replace("ï»¿", ""))
                    Toast.makeText(applicationContext, json.getString("message"), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
            }, Response.ErrorListener { error ->
                try {
                    error.printStackTrace()
                    Toast.makeText(applicationContext, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }

                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = prefs.getString("latitud", "")!!
                    parameters["longitud"] = prefs.getString("longitud", "")!!
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}