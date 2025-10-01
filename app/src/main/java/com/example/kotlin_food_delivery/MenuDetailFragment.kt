package com.example.kotlin_food_delivery

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.OkHttpClient
import org.json.JSONObject

class MenuDetailFragment : Fragment() {
    private lateinit var layout: LinearLayout
    private var mid: Int = -1
    private val locationManager = LocationManager.getInstance()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Recupera l'ID del menu dagli argomenti
        mid = arguments?.getInt("mid", -1) ?: -1

        layout =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

        val scrollView = ScrollView(requireContext()).apply { addView(layout) }

        if (mid != -1) {
            requestLocationAndLoadMenuDetail()
        } else {
            showError("ID menu non valido")
        }

        return scrollView
    }

    private fun requestLocationAndLoadMenuDetail() {
        locationManager.getCurrentLocation(
                this,
                { location: LocationManager.Location? ->
                    if (location != null) {
                        loadMenuDetail(location.lat, location.lng)
                    } else {
                        showError("Impossibile ottenere la posizione")
                    }
                }
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationManager.handlePermissionResult(
                this,
                requestCode,
                grantResults,
                { location: LocationManager.Location? ->
                    if (location != null) {
                        loadMenuDetail(location.lat, location.lng)
                    } else {
                        showError("Impossibile ottenere la posizione")
                    }
                }
        )
    }

    private fun loadMenuDetail(lat: Double, lng: Double) {
        val prefs =
                requireContext()
                        .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""

        if (sid.isEmpty()) {
            showError("Sessione non valida")
            return
        }

        ApiManager.getMenuDetails(
                mid,
                lat,
                lng,
                sid,
                object : ApiManager.MenuDetailsCallback {
                    override fun onSuccess(menuDetails: JSONObject) {
                        activity?.runOnUiThread {
                            displayMenuDetail(menuDetails, lat, lng)
                            loadMenuImage(sid)
                        }
                    }

                    override fun onError(errorMessage: String) {
                        activity?.runOnUiThread {
                            showError("Errore nel caricamento del menu: $errorMessage")
                        }
                    }
                }
        )
    }

    private fun displayMenuDetail(menuDetail: JSONObject, userLat: Double, userLng: Double) {
        layout.removeAllViews()

        // Titolo
        val titleView = TextView(requireContext()).apply {
            text = menuDetail.getString("name")
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
            // titolo arancione per contrasto sullo sfondo bianco
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange))
        }
        layout.addView(titleView)

        // Immagine quadrata del menu
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val imageSize = (screenWidth * 0.8).toInt() // 80% della larghezza schermo

        val imageView =
                ImageView(requireContext()).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(imageSize, imageSize).apply {
                                setMargins(0, 0, 0, 16)
                                gravity = android.view.Gravity.CENTER_HORIZONTAL
                            }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(0xFFE0E0E0.toInt())
                    tag = "menu_image" // Tag per identificare l'ImageView
                }
        layout.addView(imageView)

        // Prezzo
        val priceView =
                TextView(requireContext()).apply {
                    text = "Prezzo: ${FormattingUtils.formatPriceCompact(menuDetail.getDouble("price"))}"
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 8)
                    // prezzo in verde
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.price_green))
                }
        layout.addView(priceView)

        // Tempo di consegna
        val deliveryView =
                TextView(requireContext()).apply {
                    text = "Tempo di consegna: ${FormattingUtils.formatDeliveryTime(menuDetail.getInt("deliveryTime"))}"
                    textSize = 16f
                    setPadding(0, 0, 0, 8)
                    // tempo in arancione
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange))
                }
        layout.addView(deliveryView)

        // Descrizione breve
        val shortDescView =
                TextView(requireContext()).apply {
                    text = menuDetail.getString("shortDescription")
                    textSize = 16f
                    setPadding(0, 0, 0, 16)
                }
        layout.addView(shortDescView)

        // Descrizione lunga
        val longDescView =
                TextView(requireContext()).apply {
                    text = menuDetail.getString("longDescription")
                    textSize = 14f
                    setPadding(0, 0, 0, 16)
                }
        layout.addView(longDescView)

        // Posizione (converti coordinate in indirizzo)
        val location = menuDetail.getJSONObject("location")
        val lat = location.getDouble("lat")
        val lng = location.getDouble("lng")

        val locationView =
                TextView(requireContext()).apply {
                    text = "Posizione: Caricamento..."
                    textSize = 12f
                    setTextColor(0xFF666666.toInt())
                }
        layout.addView(locationView)

        // Usa la stessa logica di geocoding del MenuFragment
        getAddressFromCoordinates(lat, lng) { address ->
            activity?.runOnUiThread { locationView.text = "Posizione: $address" }
        }
        // Pulsante acquista menù
        val buyButton = android.widget.Button(requireContext()).apply {
            text = "Acquista menù"
            setOnClickListener { buyMenu(userLat, userLng) }
            // stile arancione
            val orange = androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange)
            backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
            setTextColor(android.graphics.Color.WHITE)
        }
        layout.addView(buyButton)
    }

    private fun buyMenu(deliveryLat: Double, deliveryLng: Double) {
        val prefs =
                requireContext()
                        .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""
        val uid = prefs.getInt("uid", -1)
        if (sid.isEmpty() || uid == -1) {
            showError("Sessione non valida")
            return
        }

        // Usa ApiManager per l'acquisto
        val deliveryLocation =
                JSONObject().apply {
                    put("lat", deliveryLat)
                    put("lng", deliveryLng)
                }

        ApiManager.buyMenu(
                mid,
                sid,
                deliveryLocation,
                object : ApiManager.BuyCallback {
                    override fun onSuccess(buyResponse: JSONObject) {
                        // Ottengo l'ID ordine dal server
                        val oid =
                                try {
                                    buyResponse.getInt("oid")
                                } catch (_: Exception) {
                                    -1
                                }
                        if (oid >= 0) {
                            // Recupero dettagli completi ordine
                            ApiManager.getOrder(
                                    oid,
                                    sid,
                                    object : ApiManager.OrderCallback {
                                        override fun onSuccess(order: JSONObject) {
                                            activity?.runOnUiThread {
                                                Toast.makeText(
                                                                requireContext(),
                                                                "Acquisto completato!",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                val fragment =
                                                        ActiveDeliveryFragment().apply {
                                                            arguments =
                                                                    Bundle().apply {
                                                                        putString(
                                                                                "orderData",
                                                                                order.toString()
                                                                        )
                                                                    }
                                                        }
                                                requireActivity()
                                                        .supportFragmentManager
                                                        .beginTransaction()
                                                        .replace(R.id.nav_host_fragment, fragment)
                                                        .addToBackStack(null)
                                                        .commit()
                                            }
                                        }
                                        override fun onError(errorMessage: String) {
                                            activity?.runOnUiThread {
                                                showError(
                                                        "Errore caricamento ordine: $errorMessage"
                                                )
                                            }
                                        }
                                    }
                            )
                        } else {
                            activity?.runOnUiThread { showError("ID ordine non valido") }
                        }
                    }

                    override fun onError(errorMessage: String) {
                        activity?.runOnUiThread { showError("Errore acquisto: $errorMessage") }
                    }
                }
        )
    }

    private fun loadMenuImage(sid: String) {
        ApiManager.getMenuImage(
                mid,
                sid,
                object : ApiManager.ImageCallback {
                    override fun onSuccess(imageData: JSONObject) {
                        activity?.runOnUiThread {
                            try {
                                val base64String = imageData.getString("base64")
                                displayImage(base64String)
                            } catch (e: Exception) {
                                Log.e("MenuDetailFragment", "Errore parsing immagine: ${e.message}")
                            }
                        }
                    }

                    override fun onError(errorMessage: String) {
                        Log.e("MenuDetailFragment", "Errore caricamento immagine: $errorMessage")
                    }
                }
        )
    }

    private fun displayImage(base64String: String) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val imageView = layout.findViewWithTag<ImageView>("menu_image")
            imageView?.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("MenuDetailFragment", "Errore decodifica immagine: ${e.message}")
        }
    }

    private fun showError(message: String) {
        val errorView =
                TextView(requireContext()).apply {
                    text = message
                    textSize = 16f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
        layout.removeAllViews()
        layout.addView(errorView)
    }

    // Riutilizza la logica di geocoding dal MenuFragment
    private fun getAddressFromCoordinates(lat: Double, lng: Double, callback: (String) -> Unit) {
        Log.d("MenuDetailFragment", "Tentativo geocoding per: $lat, $lng")

        if (lat == 0.0 && lng == 0.0) {
            callback("Coordinate non valide")
            return
        }

        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            callback("Coordinate fuori range")
            return
        }

        reverseGeocodeWithGoogleAPI(lat, lng, callback)
    }

    private fun reverseGeocodeWithGoogleAPI(lat: Double, lng: Double, callback: (String) -> Unit) {
        val apiKey = "AIzaSyCy8piB2kgN9bgqun4aMhkp7OXcfJTgeJE"
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey"

        val client = OkHttpClient()
        val request = okhttp3.Request.Builder().url(url).get().build()

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                callback(
                                        "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                                )
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                if (response.isSuccessful && body != null) {
                                    try {
                                        val jsonResponse = JSONObject(body)
                                        val status = jsonResponse.getString("status")

                                        if (status == "OK") {
                                            val results = jsonResponse.getJSONArray("results")
                                            if (results.length() > 0) {
                                                val firstResult = results.getJSONObject(0)
                                                val formattedAddress =
                                                        firstResult.getString("formatted_address")
                                                callback(formattedAddress)
                                                return
                                            }
                                        }
                                        callback(
                                                "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                                        )
                                    } catch (e: Exception) {
                                        callback(FormattingUtils.formatCoordinates(lat, lng))
                                    }
                                } else {
                                    callback(FormattingUtils.formatCoordinates(lat, lng))
                                }
                            }
                        }
                )
    }

    override fun onResume() {
        super.onResume()
        // Mostra la freccia nella toolbar se non siamo embed nella Home
        if (parentFragment !is HomeFragment) {
            (activity as? MainActivity)?.setBackButtonVisible(true)
        } else {
            (activity as? MainActivity)?.setBackButtonVisible(false)
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setBackButtonVisible(false)
    }
}
