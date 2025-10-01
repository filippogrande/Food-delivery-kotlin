package com.example.kotlin_food_delivery

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {
    private lateinit var layout: LinearLayout
    private lateinit var loadingView: LoadingView
    private var contentContainerId: Int = View.generateViewId()
    private var contentContainer: FrameLayout? = null
    private val locationManager = LocationManager.getInstance()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Root layout che occupa tutto lo spazio del fragment
        layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(16, 16, 16, 16)
        }

        loadingView = LoadingView(requireContext())

        // Container dove inseriamo il fragment di dettaglio (ActiveDeliveryFragment o MenuFragment).
        // Lo impostiamo con height 0 e weight 1 così occupa tutto lo spazio rimanente e il child
        // fragment può gestire internamente la mappa con peso corretto.
        contentContainer = FrameLayout(requireContext()).apply {
            id = contentContainerId
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
            )
        }

        // Aggiungiamo prima (opzionale) una area header minima, poi il container principale.
        // Manteniamo il loadingView ma non lo incapsuliamo in uno ScrollView.
        layout.addView(loadingView)
        layout.addView(contentContainer)

        // Mostra loading e avvia il caricamento ordini
        showLoading()
        loadOrders()
        Log.d("HomeFragment","layout:$layout");
        return layout
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        loadingView.fadeIn()
    }

    private fun hideLoading() {
        loadingView.fadeOut(onComplete = { loadingView.visibility = View.GONE })
    }

    private fun showError(message: String) {
        // Mostra errore sopra il content container e nasconde il container
        hideLoading()
        // Rimuovi eventuali fragment figli
        childFragmentManager.fragments.forEach {
            childFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
        val errorView = TextView(requireContext()).apply {
            text = message
            textSize = 16f
            setPadding(24, 24, 24, 24)
            setTextColor(0xFFD32F2F.toInt())
            setBackgroundColor(0xFFFFEBEE.toInt())
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        // Rimuovo tutto tranne il content container e aggiungo l'errore sopra
        layout.removeAllViews()
        layout.addView(errorView)
        // aggiungo di nuovo un content container vuoto così la UI rimane consistente
        contentContainer = FrameLayout(requireContext()).apply { id = contentContainerId }
        layout.addView(contentContainer, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ))
    }

    private fun loadOrders() {
        val prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""
        val uid = prefs.getInt("uid", -1)

        if (sid.isEmpty() || uid == -1) {
            showError("Sessione non valida. Effettua nuovamente il login.")
            return
        }

        // Usa il metodo composito per recuperare ordini e dettagli
        ApiManager.getCompletedOrdersWithDetails(
                uid,
                sid,
                requireContext(),
                object : ApiManager.CompletedOrdersCallback {
                    override fun onSuccess(orders: JSONArray) {
                        activity?.runOnUiThread {
                            hideLoading()
                            handleOrders(orders)
                        }
                    }

                    override fun onError(errorMessage: String) {
                        activity?.runOnUiThread {
                            showError("Errore caricamento ordini: $errorMessage")
                        }
                    }
                }
        )
    }

    private fun handleOrders(orders: JSONArray) {
        try {
            // Cerca il primo ordine attivo nell'array (l'ordine più vicino/in corso)
            var activeOrder: JSONObject? = null
            val activeStatuses = setOf("PENDING", "CONFIRMED", "PREPARING", "ON_DELIVERY")

            for (i in 0 until orders.length()) {
                val o = orders.getJSONObject(i)
                val status = o.optString("status")
                if (status in activeStatuses) {
                    activeOrder = o
                    break
                }
            }

            if (activeOrder != null) {
                // Inserisci ActiveDeliveryFragment come child fragment dentro la Home
                val frag = ActiveDeliveryFragment().apply {
                    arguments = Bundle().apply { putString("orderData", activeOrder.toString()) }
                }
                childFragmentManager
                        .beginTransaction()
                        .replace(contentContainerId, frag)
                        .commit()
            } else {
                // Nessun ordine attivo: mostra inline il menù più vicino (immagine grande + pulsante compra)
                showNearestMenu()
            }
        } catch (e: Exception) {
            showError("Errore nel processamento degli ordini: ${e.message}")
        }
    }

    private fun showNearestMenu() {
        val prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""
        if (sid.isEmpty()) {
            showError("Sessione non valida")
            return
        }

        // Pulisci container
        val container = contentContainer ?: return
        container.removeAllViews()

        // Ottieni posizione e poi lista menu
        locationManager.getCurrentLocation(this, { loc ->
            val fetchMenuList = { lat: Double?, lng: Double? ->
                ApiManager.getMenuList(lat, lng, sid, object : ApiManager.MenuListCallback {
                    override fun onSuccess(menuList: JSONArray) {
                        activity?.runOnUiThread {
                            if (menuList.length() == 0) {
                                showError("Nessun menù disponibile nelle vicinanze")
                                return@runOnUiThread
                            }
                            val item = menuList.getJSONObject(0)
                            displayNearestMenuItem(container, item, loc)
                        }
                    }

                    override fun onError(errorMessage: String) {
                        activity?.runOnUiThread { showError("Errore caricamento menù: $errorMessage") }
                    }
                })
            }

            if (loc != null) fetchMenuList(loc.lat, loc.lng) else fetchMenuList(null, null)
        })
    }

    private fun displayNearestMenuItem(container: FrameLayout, item: JSONObject, loc: LocationManager.Location?) {
        try {
            container.removeAllViews()

            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // Immagine grande
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            // Immagine quadrata (sempre lato = width schermo)
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(width, width)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFE0E0E0.toInt())
            }
            card.addView(imageView)

            val nameView = TextView(requireContext()).apply {
                text = item.optString("name", "-")
                textSize = 22f
                setPadding(0, 12, 0, 6)
            }
            card.addView(nameView)

            val descView = TextView(requireContext()).apply {
                text = item.optString("shortDescription", "")
                textSize = 14f
                setPadding(0, 0, 0, 8)
            }
            card.addView(descView)

            val infoView = TextView(requireContext()).apply {
                // Mostra prezzo (verde) e tempo consegna (arancione) nello stesso TextView usando spans
                val priceText = FormattingUtils.formatPriceCompact(item.optDouble("price", 0.0))
                val timeText = FormattingUtils.formatDeliveryTime(item.optInt("deliveryTime", 0))
                val separator = " • "
                val full = priceText + separator + timeText
                val spannable = android.text.SpannableString(full)
                try {
                    val priceColor = androidx.core.content.ContextCompat.getColor(context, R.color.price_green)
                    val timeColor = androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange)
                    spannable.setSpan(
                            android.text.style.ForegroundColorSpan(priceColor),
                            0,
                            priceText.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    val startTime = priceText.length + separator.length
                    spannable.setSpan(
                            android.text.style.ForegroundColorSpan(timeColor),
                            startTime,
                            startTime + timeText.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (_: Exception) {}
                text = spannable
                textSize = 14f
                setPadding(0, 0, 0, 12)
            }
            card.addView(infoView)

            val buyButton = Button(requireContext()).apply {
                text = "Acquista"
                setOnClickListener {
                    // Esegui l'acquisto usando la posizione corrente se disponibile
                    val mid = item.optInt("mid", -1)
                    if (mid < 0) {
                        Toast.makeText(requireContext(), "ID menù non valido", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    // Ottieni posizione (fallback a loc param)
                    locationManager.getCurrentLocation(this@HomeFragment, { userLoc ->
                        val deliveryLat = userLoc?.lat ?: loc?.lat ?: 45.4642
                        val deliveryLng = userLoc?.lng ?: loc?.lng ?: 9.1900
                        val deliveryLocation = JSONObject().apply {
                            put("lat", deliveryLat)
                            put("lng", deliveryLng)
                        }
                        val sid = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE).getString("sid", "") ?: ""
                        ApiManager.buyMenu(mid, sid, deliveryLocation, object : ApiManager.BuyCallback {
                            override fun onSuccess(result: JSONObject) {
                                // Ottieni oid e carica ordine completo
                                val oid = try { result.getInt("oid") } catch (_: Exception) { -1 }
                                if (oid >= 0) {
                                    ApiManager.getOrder(oid, sid, object : ApiManager.OrderCallback {
                                        override fun onSuccess(order: JSONObject) {
                                            activity?.runOnUiThread {
                                                // Mostra ActiveDeliveryFragment come child
                                                val frag = ActiveDeliveryFragment().apply {
                                                    arguments = Bundle().apply { putString("orderData", order.toString()) }
                                                }
                                                childFragmentManager.beginTransaction().replace(contentContainerId, frag).commit()
                                            }
                                        }

                                        override fun onError(errorMessage: String) {
                                            activity?.runOnUiThread { Toast.makeText(requireContext(), "Errore caricamento ordine: $errorMessage", Toast.LENGTH_SHORT).show() }
                                        }
                                    })
                                } else {
                                    activity?.runOnUiThread { Toast.makeText(requireContext(), "ID ordine non valido", Toast.LENGTH_SHORT).show() }
                                }
                            }

                            override fun onError(errorMessage: String) {
                                activity?.runOnUiThread { Toast.makeText(requireContext(), "Errore acquisto: $errorMessage", Toast.LENGTH_SHORT).show() }
                            }
                        })
                    })
                }
                // Forza stile arancione anche per i pulsanti creati programmaticamente
                val orange = androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange)
                backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
                setTextColor(android.graphics.Color.WHITE)
            }
            card.addView(buyButton)

            container.addView(card)

            // Carica immagine in background
            val sid = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE).getString("sid", "") ?: ""
            ApiManager.getMenuImage(item.optInt("mid", -1), sid, object : ApiManager.ImageCallback {
                override fun onSuccess(imageData: JSONObject) {
                    val base64 = imageData.optString("base64")
                    if (base64.isNullOrEmpty()) return
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        activity?.runOnUiThread { imageView.setImageBitmap(bmp) }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                override fun onError(errorMessage: String) {
                    // ignore image error
                }
            })

        } catch (e: Exception) {
            showError("Errore nel mostrare il menù: ${e.message}")
        }
    }
}
