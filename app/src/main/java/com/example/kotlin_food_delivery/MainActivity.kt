package com.example.kotlin_food_delivery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException
import okhttp3.*
import org.json.JSONObject
import android.widget.TextView
import android.view.Gravity
import androidx.core.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {
    private var backView: TextView? = null
    private var rightView: TextView? = null
    // azione custom invocata quando si preme la freccia. Se nulla -> popBackStack()
    private var backAction: (() -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Make the app 'super header' (toolbar) white
        toolbar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        // Ensure status bar icons are dark on white background on Android M+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        }
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)

        // Center a custom title inside the toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        // Title centered while keeping back arrow at start
        val titleView = TextView(this).apply {
            text = getString(R.string.home)
            setTextColor(ContextCompat.getColor(context, R.color.brand_orange))
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            // Forza il centraggio rimuovendo il padding laterale
            setPadding(0, 0, 0, 0)
        }
        // Back button on the left of the toolbar (hidden by default)
        backView = TextView(this).apply {
            text = getString(R.string.back_arrow)
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.brand_orange))
            visibility = View.GONE
            setPadding(24, 0, 24, 0)
            layoutParams = Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL)
            setOnClickListener {
                // se è stata impostata un'azione custom, usala, altrimenti fai il popBackStack
                backAction?.invoke() ?: run { supportFragmentManager.popBackStack() }
            }
        }
        // Invisible right placeholder to balance spacing (never shown)
        rightView = TextView(this).apply {
            text = getString(R.string.back_arrow)
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.brand_orange))
            // sempre invisibile per bilanciare la freccia sinistra
            visibility = View.INVISIBLE
            setPadding(24, 0, 24, 0)
            layoutParams = Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL)
        }
        // Add title after backView so the '<' appears immediately before the title
        toolbar.addView(backView)
        toolbar.addView(titleView)
        toolbar.addView(rightView)

        // Log per verificare che il codice venga eseguito
        android.util.Log.d("MainActivity", "Toolbar setup completato: backView=${backView?.visibility}, rightView=${rightView?.visibility}")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Chiamata API solo la prima volta
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        if (!prefs.contains("sid") || !prefs.contains("uid")) {
            val client = OkHttpClient()
            val request =
                    Request.Builder()
                            .url("https://develop.ewlab.di.unimi.it/mc/2425/user")
                            .post(ByteArray(0).toRequestBody(null)) // POST vuoto using extension
                            .build()

            client.newCall(request)
                    .enqueue(
                            object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    // Gestisci errore (puoi mostrare un Toast o loggare)
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        val body = response.body?.string()
                                        val json = JSONObject(body ?: "")
                                        val sid = json.getString("sid")
                                        val uid = json.getInt("uid")
                                        prefs.edit {
                                            putString("sid", sid)
                                            putInt("uid", uid)
                                        }
                                    }
                                }
                            }
                    )
        }

        // Set default fragment and header
        loadFragment(HomeFragment())
        // Update centered title text when switching tabs and hide back button on top-level
        bottomNavigationView.setOnItemSelectedListener { item ->
            val (fragment, title) =
                    when (item.itemId) {
                        R.id.nav_home -> HomeFragment() to getString(R.string.home)
                        R.id.nav_menu -> MenuFragment() to getString(R.string.menu)
                        R.id.nav_orders -> OrdersFragment() to getString(R.string.orders)
                        R.id.nav_profile -> ProfileFragment() to getString(R.string.profile)
                        else -> HomeFragment() to getString(R.string.home)
                    }
            loadFragment(fragment)
            // update title (left-aligned) shown in toolbar
            titleView.text = title
            // hide back button on top-level screens
            setBackButtonVisible(false)
            true
        }
    }

    // Public method for fragments to show/hide the toolbar back button
    fun setBackButtonVisible(visible: Boolean) {
        android.util.Log.d("MainActivity", "setBackButtonVisible chiamato con visible=$visible")
        backView?.visibility = if (visible) View.VISIBLE else View.GONE
        // rightView rimane sempre INVISIBLE per bilanciare costantemente
        android.util.Log.d("MainActivity", "Dopo setBackButtonVisible: backView=${backView?.visibility}, rightView=${rightView?.visibility}")
    }

    // Public method per impostare l'azione della freccia
    fun setBackButtonAction(action: (() -> Unit)?) {
        backAction = action
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, fragment).commit()
    }
}
