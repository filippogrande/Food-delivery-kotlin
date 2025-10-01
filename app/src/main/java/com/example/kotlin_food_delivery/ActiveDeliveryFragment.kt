package com.example.kotlin_food_delivery

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class ActiveDeliveryFragment : Fragment(), OnMapReadyCallback {
    private lateinit var layout: LinearLayout
    private lateinit var parentLayout: LinearLayout
    private lateinit var mapView: MapView
    private val locationManager = LocationManager.getInstance()

    // Riferimenti a TextView per aggiornamenti dinamici
    private lateinit var orderNumberView: TextView
    private lateinit var menuNameView: TextView

    private var googleMapInstance: GoogleMap? = null
    private var updateHandler: Handler? = null
    private val updateRunnable =
            object : Runnable {
                override fun run() {
                    refreshOrder()
                    updateHandler?.postDelayed(this, 5000)
                }
            }
    private lateinit var expectedView: TextView
    private lateinit var remainingView: TextView
    private var oid: Int = -1

    // Manteniamo l'ultimo ordine completo ricevuto dal server
    private var currentOrder: JSONObject? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Estrai OID per refresh (se passato) senza fare affidamento sul payload completo
        arguments?.getString("orderData")?.let {
            try {
                oid = JSONObject(it).getInt("oid")
            } catch (_: Exception) {}
        }

        // Imposta la navbar mostrando solo la freccia indietro (manteniamo il titolo originale)
        (activity as? MainActivity)?.let { mainActivity ->
            // Mostra solo la freccia indietro nella navbar
            mainActivity.setBackButtonVisible(true)
        }

        // Root che deve occupare tutto lo spazio disponibile quando è child della Home
        parentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Layout con i contenuti testuali (scrollabile)
        layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val scrollView = ScrollView(requireContext()).apply { addView(layout) }
        parentLayout.addView(
                scrollView,
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        )

        // MapView che occupa lo spazio rimanente (weight = 1)
        mapView = MapView(requireContext())
        parentLayout.addView(
                mapView,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Carica i dettagli completi dell'ordine dal server (chiamata esplicita by OID)
        val sid =
                requireContext()
                        .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        .getString("sid", "")
                        ?: ""
        if (oid >= 0 && sid.isNotEmpty()) {
            // Richiesta esplicita per ottenere i dettagli completi
            ApiManager.getOrder(
                    oid,
                    sid,
                    object : ApiManager.OrderCallback {
                        override fun onSuccess(order: JSONObject) {
                            // Salva ordine e aggiorna UI/map
                            currentOrder = order
                            activity?.runOnUiThread { updateFromOrder(order) }
                        }
                        override fun onError(errorMessage: String) {
                            activity?.runOnUiThread {
                                showError("Errore caricamento ordine: $errorMessage")
                            }
                        }
                    }
            )
        } else {
            showError("Dati ordine non validi")
        }

        return parentLayout
    }

    // Metodi di lifecycle per MapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMapInstance = googleMap
        // Se abbiamo già currentOrder, posizioniamo i marker immediatamente
        currentOrder?.let { placeMarkersForOrder(it, googleMap) }
        // Avvia aggiornamenti periodici
        updateHandler = Handler(Looper.getMainLooper())
        updateHandler?.post(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateHandler?.removeCallbacks(updateRunnable)
    }

    private fun refreshOrder() {
        if (oid < 0) return
        val sid =
                requireContext()
                        .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        .getString("sid", "")
                        ?: return
        ApiManager.getOrder(
                oid,
                sid,
                object : ApiManager.OrderCallback {
                    override fun onSuccess(order: JSONObject) {
                        activity?.runOnUiThread {
                            currentOrder = order
                            // Se ordine completato, torniamo al menu (comportamento semplice)
                            val status = order.optString("status")
                            if (status == "COMPLETED") {
                                // Torna alla schermata Menu (sostituisce il nav_host_fragment come negli altri punti del codice)
                                requireActivity()
                                        .supportFragmentManager
                                        .beginTransaction()
                                        .replace(R.id.nav_host_fragment, MenuFragment())
                                        .commit()
                                return@runOnUiThread
                            }

                            // Aggiorna UI e remaining time/map
                            updateFromOrder(order)
                        }
                    }
                    override fun onError(errorMessage: String) {
                        Log.e("ActiveDeliveryFragment", "Errore refresh order: $errorMessage")
                    }
                }
        )
    }

    // Aggiorna l'intera UI (testi e mappa) a partire dall'ordine completo
    private fun updateFromOrder(order: JSONObject) {
        try {
            // Evita duplicazioni nella UI
            layout.removeAllViews()

            // Numero ordine
            orderNumberView =
                    TextView(requireContext()).apply {
                        text = "Ordine #${order.optInt("oid", -1)}"
                        textSize = 20f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 16)
                    }
            layout.addView(orderNumberView)

            // Nome menu
            val menu = if (order.has("menu")) order.getJSONObject("menu") else null
            menuNameView =
                    TextView(requireContext()).apply {
                        text = "Menu: ${menu?.optString("name", "-") ?: "-"}"
                        textSize = 18f
                        setPadding(0, 0, 0, 16)
                    }
            layout.addView(menuNameView)

            // Inizializza placeholder per expected/remaining subito dopo il nome menu
            expectedView = TextView(requireContext()).apply {
                text = "Consegna prevista: -"
                textSize = 16f
                setPadding(0, 0, 0, 16)
            }
            layout.addView(expectedView)

            remainingView = TextView(requireContext()).apply {
                text = "Manca: -"
                textSize = 16f
                setPadding(0, 0, 0, 24)
            }
            layout.addView(remainingView)

             // Se non abbiamo dettagli del menu, richiedili esplicitamente usando il mid
             if (menu == null && order.has("mid")) {
                 val mid = order.optInt("mid", -1)
                 if (mid >= 0) {
                    val sid = requireContext()
                            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                            .getString("sid", "") ?: ""
                    if (sid.isNotEmpty()) {
                        // Otteniamo la posizione corrente per passare lat/lng come fa MenuDetailFragment
                        locationManager.getCurrentLocation(this, { loc ->
                            if (loc != null) {
                                ApiManager.getMenuDetails(mid, loc.lat, loc.lng, sid, object : ApiManager.MenuDetailsCallback {
                                    override fun onSuccess(menuDetails: JSONObject) {
                                        try { currentOrder?.put("menu", menuDetails) } catch (_: Exception) {}
                                        activity?.runOnUiThread {
                                            try {
                                                menuNameView.text = "Menu: ${menuDetails.optString("name", "-") }"
                                                val deliveryTime = menuDetails.optInt("deliveryTime", 0)
                                                val creationTS = order.optString("creationTimestamp", "")
                                                val inputFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                                                inputFmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                                val creationDate = try { inputFmt.parse(creationTS) } catch (_: Exception) { null }
                                                val cal = java.util.Calendar.getInstance().apply { creationDate?.let { time = it }; add(java.util.Calendar.MINUTE, deliveryTime) }
                                                val expectedTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(cal.time)
                                                expectedView.text = "Consegna prevista: $expectedTimeStr"
                                                val diffMillis = cal.timeInMillis - System.currentTimeMillis()
                                                val remainingMinutes = kotlin.math.max(java.lang.Math.ceil(diffMillis.toDouble() / 60000.0).toInt(), 0)
                                                remainingView.text = "Manca: ${FormattingUtils.formatDeliveryTime(remainingMinutes)}"
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    override fun onError(errorMessage: String) {
                                        Log.w("ActiveDeliveryFragment", "Errore getMenuDetails(mid=$mid): $errorMessage")
                                    }
                                })
                            } else {
                                // Fallback: richiedi comunque senza coordinate
                                ApiManager.getMenuDetails(mid, null, null, sid, object : ApiManager.MenuDetailsCallback {
                                    override fun onSuccess(menuDetails: JSONObject) {
                                        try { currentOrder?.put("menu", menuDetails) } catch (_: Exception) {}
                                        activity?.runOnUiThread {
                                            menuNameView.text = "Menu: ${menuDetails.optString("name", "-") }"
                                        }
                                    }

                                    override fun onError(errorMessage: String) {
                                        Log.w("ActiveDeliveryFragment", "Errore getMenuDetails(mid=$mid) fallback: $errorMessage")
                                    }
                                })
                            }
                        })
                    }
                }
            }

            // Stato
            val status = order.optString("status", "")
            val statusText = translateStatus(status)
            val statusColor = getStatusColor(status)
            val statusTextView =
                    TextView(requireContext()).apply {
                        text = "Stato: $statusText"
                        textSize = 16f
                        setTextColor(statusColor)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 16)
                    }
            layout.addView(statusTextView)

            // Consegna prevista e tempo rimanente (arrotondato per eccesso al minuto intero)
            val deliveryTime = menu?.optInt("deliveryTime", 0) ?: 0
            val creationTS = order.optString("creationTimestamp", "")
            val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFmt.timeZone = TimeZone.getTimeZone("UTC")
            val creationDate = try { inputFmt.parse(creationTS) } catch (_: Exception) { null }
            val cal = Calendar.getInstance().apply {
                creationDate?.let { time = it }
                add(Calendar.MINUTE, deliveryTime)
            }
            val expectedTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
            // Aggiorna i placeholder creati prima
            expectedView.text = "Consegna prevista: $expectedTimeStr"

            val diffMillis = cal.timeInMillis - System.currentTimeMillis()
            val remainingMinutes = maxOf(Math.ceil(diffMillis.toDouble() / 60000.0).toInt(), 0)
            remainingView.text = "Manca: ${FormattingUtils.formatDeliveryTime(remainingMinutes)}"

            // Indirizzo di consegna
            if (order.has("deliveryLocation")) {
                val deliveryLocation = order.getJSONObject("deliveryLocation")
                val lat = deliveryLocation.optDouble("lat", Double.NaN)
                val lng = deliveryLocation.optDouble("lng", Double.NaN)
                val locationView =
                        TextView(requireContext()).apply {
                            text = "Consegna a: Caricamento indirizzo..."
                            textSize = 16f
                            setPadding(0, 0, 0, 24)
                        }
                layout.addView(locationView)
                if (!lat.isNaN() && !lng.isNaN()) {
                    ApiManager.reverseGeocode(lat, lng) { address ->
                        activity?.runOnUiThread {
                            locationView.text = "Consegna a: ${address ?: "$lat, $lng"}"
                        }
                    }
                }
            }

            // Aggiorna markers sulla mappa se è pronta
            googleMapInstance?.let { placeMarkersForOrder(order, it) }

        } catch (e: Exception) {
            Log.e("ActiveDeliveryFragment", "Errore updateFromOrder: ${e.message}")
        }
    }

    private fun placeMarkersForOrder(order: JSONObject, map: GoogleMap) {
        try {
            map.clear()
            var destPos: LatLng? = null
            if (order.has("deliveryLocation")) {
                val locDest = order.getJSONObject("deliveryLocation")
                destPos = LatLng(locDest.getDouble("lat"), locDest.getDouble("lng"))
                map.addMarker(
                        MarkerOptions()
                                .position(destPos)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .title("Destinazione")
                )
            }
            var dronePos: LatLng? = null
            if (!order.isNull("currentPosition")) {
                val curr = order.getJSONObject("currentPosition")
                dronePos = LatLng(curr.getDouble("lat"), curr.getDouble("lng"))
                map.addMarker(
                        MarkerOptions()
                                .position(dronePos)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .title("Drone")
                )
            }
            // Calibra camera per includere entrambi i marker
            destPos?.let { dest ->
                val builder = LatLngBounds.builder().include(dest)
                dronePos?.let { builder.include(it) }
                val bounds = builder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } ?: run {
                // Se non c'è destinazione ma c'è posizione drone
                dronePos?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
            }
        } catch (e: Exception) {
            Log.e("ActiveDeliveryFragment", "Errore placeMarkers: ${e.message}")
        }
    }

    private fun translateStatus(status: String): String {
        return when (status) {
            "PENDING" -> "In attesa di conferma"
            "CONFIRMED" -> "Confermato"
            "PREPARING" -> "In preparazione"
            "ON_DELIVERY" -> "In consegna"
            "COMPLETED" -> "Completato"
            else -> status
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "PENDING" -> 0xFFFF9800.toInt() // Arancione
            "CONFIRMED" -> 0xFF2196F3.toInt() // Blu
            "PREPARING" -> 0xFFFFEB3B.toInt() // Giallo
            "ON_DELIVERY" -> 0xFF9C27B0.toInt() // Viola
            "COMPLETED" -> 0xFF4CAF50.toInt() // Verde
            else -> 0xFF666666.toInt() // Grigio
        }
    }

    private fun formatOrderTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return timestamp

            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("ActiveDeliveryFragment", "Errore formattazione data: ${e.message}")
            timestamp
        }
    }

    private fun showError(message: String) {
        val errorView =
                TextView(requireContext()).apply {
                    text = message
                    textSize = 16f
                    setTextColor(0xFFFF5722.toInt())
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setPadding(0, 32, 0, 32)
                }
        layout.removeAllViews()
        layout.addView(errorView)
    }
}
