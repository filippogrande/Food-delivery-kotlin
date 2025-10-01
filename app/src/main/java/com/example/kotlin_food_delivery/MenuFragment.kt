package com.example.kotlin_food_delivery

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class MenuFragment : Fragment() {
    private lateinit var layout: LinearLayout
    private val locationManager = LocationManager.getInstance()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        layout =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                }
        val scroll = ScrollView(requireContext()).apply { addView(layout) }

        val sid =
                requireContext()
                        .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        .getString("sid", "")
                        ?: ""

        if (sid.isNotEmpty()) requestLocationAndLoadMenu(sid) else showError("Sessione non valida")

        return scroll
    }

    private fun requestLocationAndLoadMenu(sid: String) {
        locationManager.getCurrentLocation(
                fragment = this,
                callback = { loc ->
                    if (loc != null) loadMenu(sid, loc.lat, loc.lng)
                    else showError("Impossibile ottenere la posizione")
                }
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val sid =
                requireContext()
                        .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        .getString("sid", "")
                        ?: ""
        if (sid.isNotEmpty()) {
            locationManager.handlePermissionResult(
                    fragment = this,
                    requestCode = requestCode,
                    grantResults = grantResults,
                    callback = { loc ->
                        if (loc != null) loadMenu(sid, loc.lat, loc.lng)
                        else showError("Permessi negati")
                    }
            )
        } else showError("Sessione non valida")
    }

    private fun showError(msg: String) {
        layout.removeAllViews()
        layout.addView(
                TextView(requireContext()).apply {
                    text = msg
                    textSize = 16f
                    setTextColor(0xFFD32F2F.toInt())
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
        )
    }

    private fun loadMenu(sid: String, lat: Double, lng: Double) {
        ApiManager.getMenuList(
                lat,
                lng,
                sid,
                object : ApiManager.MenuListCallback {
                    override fun onSuccess(menuList: JSONArray) {
                        activity?.runOnUiThread {
                            layout.removeAllViews()
                            for (i in 0 until menuList.length()) {
                                val item = menuList.getJSONObject(i)
                                val loc = item.getJSONObject("location")
                                val card =
                                        createMenuCard(
                                                mid = item.getInt("mid"),
                                                name = item.getString("name"),
                                                description = item.getString("shortDescription"),
                                                price = item.getDouble("price"),
                                                deliveryTime = item.getInt("deliveryTime"),
                                                lat = loc.getDouble("lat"),
                                                lng = loc.getDouble("lng"),
                                                imageVersion = item.optInt("imageVersion", 0)
                                        )
                                layout.addView(card)
                            }
                        }
                    }

                    override fun onError(errorMessage: String) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                            requireContext(),
                                            "Errore menu: $errorMessage",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
        )
    }

    private fun createMenuCard(
            mid: Int,
            name: String,
            description: String,
            price: Double,
            deliveryTime: Int,
            lat: Double,
            lng: Double,
            imageVersion: Int
    ): View {
        val card =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(0xFFF5F5F5.toInt())
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    .apply { setMargins(0, 0, 0, 16) }
                    setOnClickListener { openMenuDetail(mid) }
                }

        val size = (100 * resources.displayMetrics.density).toInt()
        val img =
                ImageView(requireContext()).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(size, size).apply { setMargins(0, 0, 16, 0) }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(0xFFE0E0E0.toInt())
                    // Leggero dimming iniziale (più elegante quando l'immagine è caricata)
                    alpha = 0.98f
                }
        loadMenuImageLocal(mid, imageVersion, img)
        card.addView(img)

        val content =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
        content.addView(
                TextView(requireContext()).apply {
                    text = name
                    textSize = 18f
                    setTypeface(null, Typeface.BOLD)
                }
        )
        content.addView(
                TextView(requireContext()).apply {
                    text = description
                    textSize = 14f
                    setPadding(0, 4, 0, 4)
                }
        )
        content.addView(
                // Mostra prezzo (verde) e tempo consegna (arancione) nello stesso TextView usando spans
                TextView(requireContext()).apply {
                    val priceText = FormattingUtils.formatPriceCompact(price)
                    val timeText = FormattingUtils.formatDeliveryTime(deliveryTime)
                    val separator = " • "
                    val full = priceText + separator + timeText
                    val spannable = android.text.SpannableString(full)
                    try {
                        val priceColor = androidx.core.content.ContextCompat.getColor(context, R.color.price_green)
                        val timeColor = androidx.core.content.ContextCompat.getColor(context, R.color.brand_orange)
                        // price span
                        spannable.setSpan(
                                android.text.style.ForegroundColorSpan(priceColor),
                                0,
                                priceText.length,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        // time span
                        val startTime = priceText.length + separator.length
                        spannable.setSpan(
                                android.text.style.ForegroundColorSpan(timeColor),
                                startTime,
                                startTime + timeText.length,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } catch (_: Exception) {}
                    text = spannable
                    textSize = 12f
                }
        )
        // Mostra posizione del negozio (indirizzo) sotto le info
        val locationTextView = TextView(requireContext()).apply {
            text = "Posizione: Caricamento..."
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 0)
        }
        content.addView(locationTextView)

        // Richiedi indirizzo dalle coordinate e aggiorna la TextView
        try {
            getAddressFromCoordinates(lat, lng) { address ->
                activity?.runOnUiThread { locationTextView.text = "Posizione: $address" }
            }
        } catch (_: Exception) {
            // ignore
        }
        card.addView(content)
        return card
    }

    // Memorizza le immagini localmente con nome mid_version.png. Se esiste già l'immagine per la
    // stessa version
    // la usa, altrimenti scarica e sovrascrive rimuovendo eventuali versioni precedenti.
    private fun loadMenuImageLocal(mid: Int, version: Int, imageView: ImageView) {
        val dir = File(requireContext().filesDir, "menu_images")
        if (!dir.exists()) dir.mkdirs()
        val targetFile = File(dir, "${mid}_$version.png")

        // Se l'immagine della versione richiesta è già presente, usala subito
        if (targetFile.exists()) {
            val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
            imageView.setImageBitmap(bmp)
            return
        }

        // Rimuovi eventuali immagini vecchie per lo stesso mid (libera spazio)
        dir.listFiles()?.forEach { f ->
            if (f.name.startsWith("${mid}_") && f.name != targetFile.name) {
                try {
                    f.delete()
                } catch (e: Exception) {
                    /* ignora */
                }
            }
        }

        // Scarica l'immagine dal server (se disponibile) e salvala come mid_version.png
        val sid =
                requireContext()
                        .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        .getString("sid", "")
                        ?: ""
        ApiManager.getMenuImage(
                mid,
                sid,
                object : ApiManager.ImageCallback {
                    override fun onSuccess(data: JSONObject) {
                        val base64 = data.optString("base64")
                        if (base64.isNullOrEmpty()) return
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        try {
                            targetFile.writeBytes(bytes)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            activity?.runOnUiThread {
                                imageView.setImageBitmap(bmp)
                                // Applica un leggero filtro nero semi-trasparente per "ingradiare" l'immagine
                                try {
                                    val overlay = 0x33000000 // semi-transparent black
                                    imageView.colorFilter = PorterDuffColorFilter(overlay, PorterDuff.Mode.SRC_ATOP)
                                } catch (_: Exception) {}
                            }
                        } catch (e: Exception) {
                            Log.e("MenuFragment", "Errore scrittura immagine: ${e.message}")
                        }
                    }

                    override fun onError(errorMessage: String) {
                        Log.e("MenuFragment", "Errore immagine menu $mid: $errorMessage")
                    }
                }
        )
    }

    private fun openMenuDetail(mid: Int) {
        val frag = MenuDetailFragment().apply { arguments = Bundle().apply { putInt("mid", mid) } }
        parentFragmentManager
                .beginTransaction()
                .replace(R.id.nav_host_fragment, frag)
                .addToBackStack(null)
                .commit()
    }

    private fun getAddressFromCoordinates(lat: Double, lng: Double, callback: (String) -> Unit) {
        ApiManager.reverseGeocode(lat, lng) { addr ->
            callback(addr ?: "Indirizzo non disponibile")
        }
    }
}
