package com.example.kotlin_food_delivery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import org.json.JSONArray
import org.json.JSONObject

class OrdersFragment : Fragment(), StateAware {
    private lateinit var layout: LinearLayout
    private lateinit var loadingView: LoadingView
    private var isFragmentActive = true // Per gestire il lifecycle come nel React Native
    private var currentState: UIState = UIState.Loading
    private var activeOrderCard: View? =
            null // Riferimento alla card dell'ordine attivo per aggiornamenti dinamici
    private var refreshTimer: Timer? = null // Timer per aggiornamenti automatici
    private var currentActiveOrder: JSONObject? = null // Mantiene riferimento all'ordine attivo
    private var completedOrdersList: JSONArray? =
            null // Mantiene riferimento agli ordini completati

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        isFragmentActive = true

        layout =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

        // Inizializza la vista di caricamento semplice
        loadingView = LoadingView(requireContext())

        val scrollView = ScrollView(requireContext()).apply { addView(layout) }

        // Inizia con stato di caricamento
        updateUIState(UIState.Loading)

        // Carica gli ordini
        loadOrders()

        return scrollView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentActive = false // Simile al pattern isMountedRef del React Native

        // Ferma il timer di refresh per evitare memory leak
        refreshTimer?.cancel()
        refreshTimer = null
        activeOrderCard = null
    }

    override fun updateUIState(state: UIState) {
        if (!isFragmentActive) return

        currentState = state

        runOnUiThreadIfActive {
            when (state) {
                is UIState.Loading -> showLoadingState()
                is UIState.Progress -> showProgressState(state)
                is UIState.Success -> showSuccessState()
                is UIState.Error -> showErrorState(state)
                is UIState.Empty -> showEmptyState()
            }
        }
    }

    private fun loadOrders() {
        val prefs =
                requireContext()
                        .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""
        val uid = prefs.getInt("uid", -1)

        if (sid.isEmpty() || uid == -1) {
            updateUIState(UIState.Error("Sessione non valida. Effettua nuovamente il login."))
            return
        }

        updateUIState(UIState.Progress(1, 1, "Caricamento ordini..."))

        // Usa il metodo composito dell'ApiManager
        ApiManager.getCompletedOrdersWithDetails(
                uid,
                sid,
                requireContext(),
                object : ApiManager.CompletedOrdersCallback {
                    override fun onSuccess(orders: JSONArray) {
                        Log.d("OrdersFragment", "✅ ${orders.length()} ordini totali ricevuti")

                        runOnUiThreadIfActive {
                            displayOrders(orders)
                            updateUIState(UIState.Success())
                        }
                    }

                    override fun onError(errorMessage: String) {
                        Log.e("OrdersFragment", "❌ Errore caricamento ordini: $errorMessage")

                        val error = ErrorHandler.createNetworkError(errorMessage)
                        ErrorHandler.logError(error, "OrdersFragment")

                        updateUIState(UIState.Error(ErrorHandler.getUserMessage(error), true))
                    }
                }
        )
    }

    private fun showLoadingState() {
        layout.removeAllViews()
        layout.addView(loadingView)
        loadingView.fadeIn()
    }

    private fun showProgressState(state: UIState.Progress) {
        // Per la versione semplice, mostra solo la rotellina
        // In futuro si potrebbe usare state.current, state.total, state.message per una barra di
        // progresso
        showLoadingState()
    }

    private fun showSuccessState() {
        loadingView.fadeOut {
            // Il contenuto è già stato aggiunto da displayOrdersOptimized
        }
    }

    private fun showErrorState(state: UIState.Error) {
        layout.removeAllViews()

        val errorView =
                TextView(requireContext()).apply {
                    text = state.message
                    textSize = 16f
                    setPadding(24, 24, 24, 24)
                    setTextColor(0xFFD32F2F.toInt()) // Rosso per errori
                    setBackgroundColor(0xFFFFEBEE.toInt()) // Sfondo rosa chiaro
                    setLineSpacing(4f, 1.1f)
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                }

        layout.addView(errorView)

        if (state.canRetry) {
            // Aggiungi pulsante retry se necessario
            val retryButton =
                    TextView(requireContext()).apply {
                        text = "Riprova"
                        textSize = 16f
                        setPadding(32, 16, 32, 16)
                        setTextColor(0xFFFC6444.toInt())
                        setBackgroundColor(0xFFFFFFFF.toInt())
                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        setOnClickListener { loadOrders() }
                    }
            layout.addView(retryButton)
        }
    }

    private fun showEmptyState() {
        layout.removeAllViews()

        val emptyView =
                TextView(requireContext()).apply {
                    text = "Nessun ordine trovato"
                    textSize = 18f
                    setPadding(24, 50, 24, 24)
                    setTextColor(0xFF666666.toInt())
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                }

        layout.addView(emptyView)
    }

    private fun displayOrders(allOrders: JSONArray) {
        // Salva i dati per il refresh automatico
        if (allOrders.length() > 0) {
            val firstOrder = allOrders.getJSONObject(0)
            val firstOrderStatus = firstOrder.optString("status")

            // Il primo ordine è quello corrente se è in uno stato attivo
            val isCurrentOrder =
                    firstOrderStatus in listOf("PENDING", "CONFIRMED", "PREPARING", "ON_DELIVERY")

            if (isCurrentOrder) {
                currentActiveOrder = firstOrder
                // Crea array degli ordini completati (tutti tranne il primo)
                completedOrdersList =
                        JSONArray().apply {
                            for (i in 1 until allOrders.length()) {
                                put(allOrders.getJSONObject(i))
                            }
                        }
            } else {
                // Nessun ordine corrente attivo, tutti sono completati
                currentActiveOrder = null
                completedOrdersList = allOrders
            }
        } else {
            currentActiveOrder = null
            completedOrdersList = JSONArray()
        }

        val totalOrders = allOrders.length()
        val hasCurrentOrder = currentActiveOrder != null

        if (totalOrders == 0) {
            updateUIState(UIState.Empty)
            return
        }

        // Rimuovi solo la loading view, mantieni il resto del layout
        layout.removeView(loadingView)

        // Titolo
        val titleView =
                TextView(requireContext()).apply {
                    text = "I tuoi ordini ($totalOrders)"
                    textSize = 24f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 24)
                }
        layout.addView(titleView)

        // Mostra tutti gli ordini dall'array composito
        for (i in 0 until allOrders.length()) {
            val order = allOrders.getJSONObject(i)
            val orderStatus = order.optString("status")
            val isCurrentOrder =
                    orderStatus in listOf("PENDING", "CONFIRMED", "PREPARING", "ON_DELIVERY")
            val orderCard = createOrderCard(order, isCurrentOrder = isCurrentOrder)
            layout.addView(orderCard)

            // Conserva riferimento per aggiornamenti dinamici se è un ordine attivo
            if (isCurrentOrder) {
                activeOrderCard = orderCard
            }
        }
    }

    private fun runOnUiThreadIfActive(action: () -> Unit) {
        if (isFragmentActive) {
            activity?.runOnUiThread(action)
        }
    }

    /** Forza il refresh dei dati (senza cache) */
    private fun refreshOrders() {
        val prefs =
                requireContext()
                        .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
        val sid = prefs.getString("sid", "") ?: ""
        val uid = prefs.getInt("uid", -1)

        if (sid.isEmpty() || uid == -1) return

        // Usa il metodo composito per il refresh
        ApiManager.getCompletedOrdersWithDetails(
                uid,
                sid,
                requireContext(),
                object : ApiManager.CompletedOrdersCallback {
                    override fun onSuccess(orders: JSONArray) {
                        Log.d("OrdersFragment", "🔄 Refresh completato: ${orders.length()} ordini")
                        runOnUiThreadIfActive { displayOrders(orders) }
                    }

                    override fun onError(errorMessage: String) {
                        Log.e("OrdersFragment", "❌ Errore refresh: $errorMessage")
                        // Mantieni i dati esistenti in caso di errore del refresh
                    }
                }
        )
    }

    override fun onResume() {
        super.onResume()
        // Ricarica solo se i dati sono vecchi (cache automatica)
        if (::layout.isInitialized && layout.childCount == 0) {
            loadOrders()
        }
    }

    private fun createOrderCard(order: JSONObject, isCurrentOrder: Boolean = false): View {
        // Root frame per permettere overlay dello status badge
        val cardFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
            // Aggiungiamo una piccola background interna al contenuto invece che al frame
        }

        // Contenuto principale verticale
        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            // Usa colore diverso per l'ordine corrente
            setBackgroundColor(if (isCurrentOrder) 0xFFE3F2FD.toInt() else 0xFFF5F5F5.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
            foreground = android.graphics.drawable.ColorDrawable(0x1F000000)
            setOnClickListener { openDeliveryStatus(order) }
        }

        // Ottieni i dati del menu - ora sono annidati nell'ordine (logging ridotto)
        val menu =
                if (order.has("menu")) {
                    order.getJSONObject("menu")
                } else {
                    // Fallback per ordini senza dettagli del menu
                    // Prova a estrarre l'ID del menu dall'ordine stesso
                    val menuId = if (order.has("mid")) order.getInt("mid") else -1
                    JSONObject().apply {
                        put(
                                "name",
                                if (menuId != -1) "Menu ID $menuId (dettagli non disponibili)"
                                else "Menu sconosciuto"
                        )
                        put("price", 0.0)
                        if (menuId != -1) put("mid", menuId)
                    }
                }

        // 1. Ordine: (numero)
        val orderNumberView = TextView(requireContext()).apply {
            text = "Ordine: ${order.getInt("oid")}"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 4)
        }
        contentLayout.addView(orderNumberView)

        // 3. Nome menu
        val menuNameView = TextView(requireContext()).apply {
            val menuName = menu.getString("name")
            text = menuName
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 4)
        }
        contentLayout.addView(menuNameView)

        // 4. Data di creazione dell'ordine (se disponibile)
        if (order.has("creationTimestamp")) {
            val creationTs = order.optString("creationTimestamp", "")
            val creationView = TextView(requireContext()).apply {
                text = try { formatDate(creationTs) } catch (_: Exception) { creationTs }
                textSize = 12f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 0, 0, 4)
            }
            contentLayout.addView(creationView)
        }

        // 5. Prezzo (se disponibile nel menu o nell'ordine)
        val priceValue = try {
            if (menu.has("price")) menu.optDouble("price", 0.0) else order.optDouble("price", 0.0)
        } catch (_: Exception) { 0.0 }
        val priceView = TextView(requireContext()).apply {
            text = FormattingUtils.formatPriceCompact(priceValue)
            textSize = 14f
            // colore verde per il prezzo
            setTextColor(ContextCompat.getColor(requireContext(), R.color.price_green))
            setPadding(0, 0, 0, 4)
        }
        contentLayout.addView(priceView)

        // Costruzione del badge di stato (pillola) che sarà sovrapposta in alto a destra
        val status = order.optString("status")
        val statusText = translateStatus(status)
        val badge = TextView(requireContext()).apply {
            text = statusText
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(20, 8, 20, 8)
            // colore di background in base allo status
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(getStatusColor(status))
            }
            background = bg
            // posizione verrà impostata nel layout params
        }

        // Aggiungi il contenuto e il badge al frame
        cardFrame.addView(contentLayout)
        val badgeLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.END or Gravity.TOP
            setMargins(0, 8, 8, 0)
        }
        cardFrame.addView(badge, badgeLp)

        return cardFrame
    }

    private fun translateStatus(status: String): String {
        return when (status) {
            "ON_DELIVERY" -> "In consegna"
            "COMPLETED" -> "Completato"
            else -> status
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "ON_DELIVERY" -> 0xFF9C27B0.toInt() // Viola
            "COMPLETED" -> 0xFF4CAF50.toInt() // Verde
            else -> 0xFF666666.toInt() // Grigio
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString) ?: return dateString

            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }

            calendar.time = date

            when {
                // Stesso giorno di oggi
                calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "Oggi ${timeFormat.format(date)}"
                }
                // Ieri
                calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) ==
                                yesterday.get(Calendar.DAY_OF_YEAR) -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "Ieri ${timeFormat.format(date)}"
                }
                // Data completa per giorni precedenti
                else -> {
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            Log.e("OrdersFragment", "Errore formattazione data: ${e.message}")
            dateString
        }
    }

    private fun openDeliveryStatus(order: JSONObject) {
        try {
            val status = order.getString("status")
            val fragment = if (status in listOf("PENDING", "CONFIRMED", "PREPARING", "ON_DELIVERY")) {
                // Ordine attivo - usa ActiveDeliveryFragment
                ActiveDeliveryFragment()
            } else {
                // Ordine completato - usa DeliveryStatusFragment  
                DeliveryStatusFragment()
            }

            // Passa i dati dell'ordine come argomenti
            val bundle = Bundle().apply {
                putString("orderData", order.toString())
            }
            fragment.arguments = bundle

            // Naviga al fragment
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()

        } catch (e: Exception) {
            Log.e("OrdersFragment", "Errore apertura stato consegna: ${e.message}")
            Toast.makeText(context, "Errore nell'apertura dello stato consegna", Toast.LENGTH_SHORT).show()
        }
    }
}
