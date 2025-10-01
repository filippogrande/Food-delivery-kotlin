package com.example.kotlin_food_delivery

import android.os.Bundle
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class DeliveryStatusFragment : Fragment(), OnMapReadyCallback {
    private lateinit var layout: LinearLayout
    private lateinit var parentLayout: LinearLayout
    private lateinit var mapView: MapView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        layout =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }
        parentLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val scrollView = ScrollView(requireContext()).apply { addView(layout) }
        parentLayout.addView(
                scrollView,
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        )
        mapView = MapView(requireContext())
        parentLayout.addView(
                mapView,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val orderData = arguments?.getString("orderData")
        if (orderData != null) {
            try {
                val order = JSONObject(orderData)
                loadCompletedOrderStatus(order)
            } catch (e: Exception) {
                Log.e("DeliveryStatusFragment", "Error parsing order: ${e.message}")
                showError("Errore nel caricamento dei dati")
            }
        } else {
            showError("Nessun dato ordine disponibile")
        }
        return parentLayout
    }

    private fun loadCompletedOrderStatus(order: JSONObject) {
        val title =
                TextView(requireContext()).apply {
                    text = "Consegna Completata"
                    textSize = 24f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 24)
                }
        layout.addView(title)
        val orderNum =
                TextView(requireContext()).apply {
                    text = "Ordine #${order.optInt("oid")}"
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 16)
                }
        layout.addView(orderNum)
        val menuName = order.optJSONObject("menu")?.optString("name") ?: "N/A"
        val menuView =
                TextView(requireContext()).apply {
                    text = "Menu: $menuName"
                    textSize = 16f
                    setPadding(0, 0, 0, 16)
                }
        layout.addView(menuView)
        if (order.has("deliveryTimestamp")) {
            val at = formatTimestamp(order.getString("deliveryTimestamp"))
            val deliveredView =
                    TextView(requireContext()).apply {
                        text = "Consegnato: $at"
                        textSize = 16f
                        setPadding(0, 0, 0, 16)
                    }
            layout.addView(deliveredView)
        }
        if (order.has("deliveryLocation")) {
            val loc = order.getJSONObject("deliveryLocation")
            val lat = loc.optDouble("lat")
            val lng = loc.optDouble("lng")
            val locView =
                    TextView(requireContext()).apply {
                        text = "Consegna a: Caricamento indirizzo..."
                        textSize = 16f
                        setPadding(0, 0, 0, 24)
                    }
            layout.addView(locView)
            ApiManager.reverseGeocode(lat, lng) { address ->
                activity?.runOnUiThread { locView.text = "Consegna a: ${address ?: "$lat, $lng"}" }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            arguments?.getString("orderData")?.let {
                val order = JSONObject(it)
                val loc = order.getJSONObject("deliveryLocation")
                val lat = loc.getDouble("lat")
                val lng = loc.getDouble("lng")
                val pos = LatLng(lat, lng)
                map.addMarker(MarkerOptions().position(pos))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            }
        } catch (e: Exception) {
            Log.e("DeliveryStatusFragment", "onMapReady error: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Mostra la freccia nella toolbar e imposta l'azione per tornare a "I miei ordini"
        (activity as? MainActivity)?.setBackButtonVisible(true)
        (activity as? MainActivity)?.setBackButtonAction {
            // torna allo stack precedente (solitamente OrdersFragment)
            requireActivity().supportFragmentManager.popBackStack()
        }
        mapView.onResume()
    }
    override fun onPause() {
        super.onPause()
        // Nascondi la freccia e rimuovi l'azione per evitare side-effect
        (activity as? MainActivity)?.setBackButtonVisible(false)
        (activity as? MainActivity)?.setBackButtonAction(null)
        mapView.onPause()
    }
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun formatTimestamp(ts: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inFmt.timeZone = TimeZone.getTimeZone("UTC")
            val d = inFmt.parse(ts) ?: return ts
            val outFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outFmt.format(d)
        } catch (e: Exception) {
            ts
        }
    }

    private fun showError(msg: String) {
        layout.removeAllViews()
        layout.addView(
                TextView(requireContext()).apply {
                    text = msg
                    textSize = 16f
                    setTextColor(0xFFD32F2F.toInt())
                    setPadding(0, 32, 0, 32)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
        )
    }
}
