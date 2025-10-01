package com.example.kotlin_food_delivery

import android.content.Context
import android.location.LocationManager
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manager centralizzato per tutte le chiamate API al server. Ogni metodo corrisponde esattamente a
 * un endpoint della documentazione API.
 */
object ApiManager {
    private val client = OkHttpClient()
    private const val BASE_URL = "https://develop.ewlab.di.unimi.it/mc/2425"

    // Interfacce per i callback
    interface UserCallback {
        fun onSuccess(user: JSONObject)
        fun onError(errorMessage: String)
    }

    interface OrderCallback {
        fun onSuccess(order: JSONObject)
        fun onError(errorMessage: String)
    }

    interface CompletedOrdersCallback {
        fun onSuccess(orders: JSONArray)
        fun onError(errorMessage: String)
    }

    interface MenuListCallback {
        fun onSuccess(menuList: JSONArray)
        fun onError(errorMessage: String)
    }

    interface MenuDetailsCallback {
        fun onSuccess(menuDetails: JSONObject)
        fun onError(errorMessage: String)
    }

    interface BuyCallback {
        fun onSuccess(result: JSONObject)
        fun onError(errorMessage: String)
    }

    interface ImageCallback {
        fun onSuccess(imageData: JSONObject)
        fun onError(errorMessage: String)
    }

    // ========== ENDPOINTS UTENTE ==========

    /** POST /user - Registra o effettua login di un utente */
    fun createUser(userData: JSONObject, callback: UserCallback) {
        val url = "$BASE_URL/user"
        val requestBody = userData.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        Log.d("ApiManager", "📝 POST /user: $url")
        Log.d("ApiManager", "📝 Body: $userData")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore POST /user: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "📝 Risposta POST /user - Code: ${response.code}, Body: $body"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val result = JSONObject(body)
                                        callback.onSuccess(result)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing POST /user: ${e.message}"
                                        )
                                        callback.onError("Errore parsing risposta: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP POST /user: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** GET /user/{uid} - Recupera il profilo di un utente */
    fun getUser(uid: Int, sid: String, callback: UserCallback) {
        val url = "$BASE_URL/user/$uid?sid=$sid"
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "👤 GET /user/$uid: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET /user/$uid: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "👤 Risposta GET /user/$uid - Code: ${response.code}, Body: $body"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val user = JSONObject(body)
                                        callback.onSuccess(user)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing GET /user/$uid: ${e.message}"
                                        )
                                        callback.onError("Errore parsing profilo: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP GET /user/$uid: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** PUT /user/{uid} - Aggiorna il profilo di un utente */
    fun updateUser(uid: Int, sid: String, userData: JSONObject, callback: UserCallback) {
        val url = "$BASE_URL/user/$uid"
        val requestBody = userData.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).put(requestBody).build()

        Log.d("ApiManager", "✏️ PUT /user/$uid: $url")
        Log.d("ApiManager", "✏️ Body: $userData")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore PUT /user/$uid: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "✏️ Risposta PUT /user/$uid - Code: ${response.code}, Body: '$body'"
                                )

                                if (response.isSuccessful) {
                                    try {
                                        // Se la risposta è vuota o null, considerala un successo
                                        if (body.isNullOrEmpty() || body.trim().isEmpty()) {
                                            Log.d("ApiManager", "✅ PUT success - risposta vuota")
                                            val successResponse =
                                                    JSONObject().apply {
                                                        put("success", true)
                                                        put(
                                                                "message",
                                                                "Aggiornamento completato con successo"
                                                        )
                                                        put("uid", uid)
                                                    }
                                            callback.onSuccess(successResponse)
                                        } else {
                                            val result = JSONObject(body)
                                            callback.onSuccess(result)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing PUT /user/$uid: ${e.message}"
                                        )
                                        callback.onError("Errore parsing risposta: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP PUT /user/$uid: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** GET /user/febbraio2/{uid}/completedorders - Recupera gli ordini completati */
    fun getCompletedOrders(uid: Int, sid: String, callback: CompletedOrdersCallback) {
        val url = "$BASE_URL/user/febbraio2/$uid/completedorders?sid=$sid"
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "📋 GET /user/febbraio2/$uid/completedorders: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET completeorders: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "📋 Risposta completeorders - Code: ${response.code}, Body: ${body?.take(200)}..."
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val orders = JSONArray(body)
                                        Log.d(
                                                "ApiManager",
                                                "✅ Ordini completati caricati: ${orders.length()}"
                                        )
                                        callback.onSuccess(orders)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing completeorders: ${e.message}"
                                        )
                                        callback.onError("Errore parsing ordini: ${e.message}")
                                    }
                                } else {
                                    val fullErrorDetails =
                                            """
🔥 ERRORE COMPLETEDORDERS:
📍 URL: $url
🔗 Metodo: GET
📊 Response Code: ${response.code}
📝 Response Message: ${response.message}
📄 Response Body: $body
                                    """.trimIndent()

                                    Log.e("ApiManager", fullErrorDetails)
                                    callback.onError(fullErrorDetails)
                                }
                            }
                        }
                )
    }

    // ========== ENDPOINTS ORDINI ==========

    /** GET /order/{oid} - Recupera i dettagli di un ordine */
    fun getOrder(oid: Int, sid: String, callback: OrderCallback) {
        val url = "$BASE_URL/order/$oid?sid=$sid"
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "📦 GET /order/$oid: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET /order/$oid: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "📦 Risposta GET /order/$oid - Code: ${response.code}, Body: $body"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val order = JSONObject(body)
                                        callback.onSuccess(order)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing GET /order/$oid: ${e.message}"
                                        )
                                        callback.onError("Errore parsing ordine: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP GET /order/$oid: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** POST /menu/{mid}/buy - Acquista un menu */
    fun buyMenu(mid: Int, sid: String, deliveryLocation: JSONObject, callback: BuyCallback) {
        val url = "$BASE_URL/menu/$mid/buy"
        val requestBody =
                JSONObject()
                        .apply {
                            put("sid", sid)
                            put("deliveryLocation", deliveryLocation)
                        }
                        .toString()
                        .toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(requestBody).build()

        Log.d("ApiManager", "🛒 POST /menu/$mid/buy: $url")
        Log.d("ApiManager", "🛒 DeliveryLocation: $deliveryLocation")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore POST /menu/$mid/buy: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "🛒 Risposta POST /menu/$mid/buy - Code: ${response.code}, Body: $body"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val result = JSONObject(body)
                                        callback.onSuccess(result)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing POST /menu/$mid/buy: ${e.message}"
                                        )
                                        callback.onError("Errore parsing risposta: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e(
                                            "ApiManager",
                                            "🔥 Errore HTTP POST /menu/$mid/buy: $errorMsg"
                                    )
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    // ========== ENDPOINTS MENU ==========

    /** GET /menu - Recupera la lista dei menu disponibili */
    fun getMenuList(
            lat: Double? = null,
            lng: Double? = null,
            sid: String,
            callback: MenuListCallback
    ) {
        val url =
                if (lat != null && lng != null) {
                    "$BASE_URL/menu?lat=$lat&lng=$lng&sid=$sid"
                } else {
                    "$BASE_URL/menu?sid=$sid"
                }
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "🍽️ GET /menu: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET /menu: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "🍽️ Risposta GET /menu - Code: ${response.code}, Body length: ${body?.length}"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val menuList = JSONArray(body)
                                        Log.d(
                                                "ApiManager",
                                                "✅ Lista menu caricata: ${menuList.length()} elementi"
                                        )
                                        callback.onSuccess(menuList)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing GET /menu: ${e.message}"
                                        )
                                        callback.onError("Errore parsing menu: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP GET /menu: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** GET /menu/{mid} - Recupera i dettagli di un menu specifico */
    fun getMenuDetails(
            mid: Int,
            lat: Double? = null,
            lng: Double? = null,
            sid: String,
            callback: MenuDetailsCallback
    ) {
        val url =
                if (lat != null && lng != null) {
                    "$BASE_URL/menu/$mid?lat=$lat&lng=$lng&sid=$sid"
                } else {
                    "$BASE_URL/menu/$mid?sid=$sid"
                }
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "🍽️ GET /menu/$mid: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET /menu/$mid: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "🍽️ Risposta GET /menu/$mid - Code: ${response.code}, Body: $body"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val menuDetails = JSONObject(body)
                                        callback.onSuccess(menuDetails)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing GET /menu/$mid: ${e.message}"
                                        )
                                        callback.onError("Errore parsing menu: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e("ApiManager", "🔥 Errore HTTP GET /menu/$mid: $errorMsg")
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    /** GET /menu/{mid}/image - Recupera l'immagine di un menu */
    fun getMenuImage(mid: Int, sid: String, callback: ImageCallback) {
        val url = "$BASE_URL/menu/$mid/image?sid=$sid"
        val request = Request.Builder().url(url).get().build()

        Log.d("ApiManager", "🖼️ GET /menu/$mid/image: $url")

        client.newCall(request)
                .enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                Log.e("ApiManager", "🔥 Errore GET /menu/$mid/image: ${e.message}")
                                callback.onError("Errore di rete: ${e.message}")
                            }

                            override fun onResponse(
                                    call: okhttp3.Call,
                                    response: okhttp3.Response
                            ) {
                                val body = response.body?.string()
                                Log.d(
                                        "ApiManager",
                                        "🖼️ Risposta GET /menu/$mid/image - Code: ${response.code}, Body length: ${body?.length}"
                                )

                                if (response.isSuccessful && body != null) {
                                    try {
                                        val imageData = JSONObject(body)
                                        callback.onSuccess(imageData)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "ApiManager",
                                                "🔥 Errore parsing GET /menu/$mid/image: ${e.message}"
                                        )
                                        callback.onError("Errore parsing immagine: ${e.message}")
                                    }
                                } else {
                                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                                    Log.e(
                                            "ApiManager",
                                            "🔥 Errore HTTP GET /menu/$mid/image: $errorMsg"
                                    )
                                    callback.onError(errorMsg)
                                }
                            }
                        }
                )
    }

    // ========== METODI DI COMPATIBILITÀ ==========
    // Per mantenere compatibilità con il codice esistente

    @Deprecated("Usa getUser() invece", ReplaceWith("getUser(uid, sid, callback)"))
    fun getUserProfile(uid: Int, sid: String, callback: UserCallback) = getUser(uid, sid, callback)

    @Deprecated("Usa getOrder() invece", ReplaceWith("getOrder(oid, sid, callback)"))
    fun getOrderDetails(oid: Int, sid: String, callback: OrderCallback) =
            getOrder(oid, sid, callback)

    /** Crea un ordine completo combinando dettagli ordine e menu */
    fun createCompleteOrder(orderDetails: JSONObject, menuDetails: JSONObject): JSONObject {
        return JSONObject().apply {
            // Dati dell'ordine
            put("oid", orderDetails.getInt("oid"))
            put("uid", orderDetails.getInt("uid"))
            put("status", orderDetails.getString("status"))
            put("creationTimestamp", orderDetails.getString("creationTimestamp"))
            put("deliveryLocation", orderDetails.getJSONObject("deliveryLocation"))

            // Aggiungi deliveryTimestamp se presente
            if (orderDetails.has("deliveryTimestamp") && !orderDetails.isNull("deliveryTimestamp")
            ) {
                put("deliveryTimestamp", orderDetails.getString("deliveryTimestamp"))
            }

            // Aggiungi currentPosition se presente
            if (orderDetails.has("currentPosition") && !orderDetails.isNull("currentPosition")) {
                put("currentPosition", orderDetails.getJSONObject("currentPosition"))
            }

            // Aggiungi mid dall'ordine
            put("mid", orderDetails.getInt("mid"))

            // Dati del menu
            put("menu", menuDetails)
        }
    }

    /** Recupera tutti gli ordini completati dell'utente (ordine corrente + storico) */
    fun getCompletedOrdersWithDetails(
            uid: Int,
            sid: String,
            context: Context,
            callback: CompletedOrdersCallback
    ) {
        // Ottieni la posizione GPS del telefono
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var userLat: Double? = null
        var userLng: Double? = null

        try {
            // Prova a ottenere l'ultima posizione conosciuta (GPS o Network)
            val lastKnownLocationGPS =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownLocationNetwork =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val location = lastKnownLocationGPS ?: lastKnownLocationNetwork
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
                Log.d("ApiManager", "📍 Posizione GPS ottenuta: lat=$userLat, lng=$userLng")
            } else {
                Log.w(
                        "ApiManager",
                        "⚠️ Nessuna posizione GPS disponibile, continuo senza coordinate"
                )
            }
        } catch (e: SecurityException) {
            Log.w("ApiManager", "⚠️ Permessi GPS non disponibili: ${e.message}")
        }

        Log.d("ApiManager", "🔄 Recupero ordini completi per uid=$uid, lat=$userLat, lng=$userLng")

        // Prima recupera l'utente per ottenere l'ordine corrente
        getUser(
                uid,
                sid,
                object : UserCallback {
                    override fun onSuccess(user: JSONObject) {
                        Log.d("ApiManager", "👤 Utente recuperato, controllo ordine corrente...")

                        val hasLastOid = user.has("lastOid") && !user.isNull("lastOid")
                        val hasOrderStatus = user.has("orderStatus") && !user.isNull("orderStatus")

                        if (hasLastOid && hasOrderStatus) {
                            val currentOid = user.getInt("lastOid")
                            val orderStatus = user.getString("orderStatus")

                            if (orderStatus in
                                            listOf(
                                                    "PENDING",
                                                    "CONFIRMED",
                                                    "PREPARING",
                                                    "ON_DELIVERY"
                                            )
                            ) {
                                Log.d(
                                        "ApiManager",
                                        "📦 Ordine corrente attivo trovato: oid=$currentOid, status=$orderStatus"
                                )

                                getOrder(
                                        currentOid,
                                        sid,
                                        object : OrderCallback {
                                            override fun onSuccess(orderDetails: JSONObject) {
                                                val currentMid = orderDetails.getInt("mid")
                                                Log.d(
                                                        "ApiManager",
                                                        "📦 Dettagli ordine corrente: mid=$currentMid"
                                                )

                                                // Usa la posizione GPS del telefono passata come
                                                // parametro
                                                getMenuDetails(
                                                        currentMid,
                                                        userLat,
                                                        userLng,
                                                        sid,
                                                        object : MenuDetailsCallback {
                                                            override fun onSuccess(
                                                                    menuDetails: JSONObject
                                                            ) {
                                                                Log.d(
                                                                        "ApiManager",
                                                                        "🍽️ Menu dell'ordine corrente recuperato"
                                                                )
                                                                val currentOrder =
                                                                        JSONObject().apply {
                                                                            put("oid", currentOid)
                                                                            put("uid", uid)
                                                                            put(
                                                                                    "status",
                                                                                    orderStatus
                                                                            ) // Usa lo status reale
                                                                            // (PREPARING,
                                                                            // ON_DELIVERY, ecc.)
                                                                            put("mid", currentMid)
                                                                            put("menu", menuDetails)
                                                                            if (orderDetails.has(
                                                                                            "deliveryLocation"
                                                                                    )
                                                                            ) {
                                                                                put(
                                                                                        "deliveryLocation",
                                                                                        orderDetails
                                                                                                .getJSONObject(
                                                                                                        "deliveryLocation"
                                                                                                )
                                                                                )
                                                                            }
                                                                            if (orderDetails.has(
                                                                                            "creationTimestamp"
                                                                                    )
                                                                            ) {
                                                                                put(
                                                                                        "creationTimestamp",
                                                                                        orderDetails
                                                                                                .getString(
                                                                                                        "creationTimestamp"
                                                                                                )
                                                                                )
                                                                            }
                                                                            if (orderDetails.has(
                                                                                            "deliveryTimestamp"
                                                                                    )
                                                                            ) {
                                                                                put(
                                                                                        "deliveryTimestamp",
                                                                                        orderDetails
                                                                                                .getString(
                                                                                                        "deliveryTimestamp"
                                                                                                )
                                                                                )
                                                                            }
                                                                            if (orderDetails.has(
                                                                                            "currentPosition"
                                                                                    )
                                                                            ) {
                                                                                put(
                                                                                        "currentPosition",
                                                                                        orderDetails
                                                                                                .getJSONObject(
                                                                                                        "currentPosition"
                                                                                                )
                                                                                )
                                                                            }
                                                                        }
                                                                getCompletedOrders(
                                                                        uid,
                                                                        sid,
                                                                        object :
                                                                                CompletedOrdersCallback {
                                                                            override fun onSuccess(
                                                                                    allHistoricOrders:
                                                                                            JSONArray
                                                                            ) {
                                                                                Log.d(
                                                                                        "ApiManager",
                                                                                        "📋 ${allHistoricOrders.length()} ordini storici recuperati"
                                                                                )

                                                                                val allOrders =
                                                                                        JSONArray()
                                                                                                .apply {
                                                                                                    put(
                                                                                                            currentOrder
                                                                                                    )
                                                                                                    for (i in
                                                                                                            0 until
                                                                                                                    allHistoricOrders
                                                                                                                            .length()) {
                                                                                                        put(
                                                                                                                allHistoricOrders
                                                                                                                        .getJSONObject(
                                                                                                                                i
                                                                                                                        )
                                                                                                        )
                                                                                                    }
                                                                                                }
                                                                                Log.d(
                                                                                        "ApiManager",
                                                                                        "✅ Lista completa creata: ${allOrders.length()} ordini totali (1 attivo + ${allHistoricOrders.length()} completati)"
                                                                                )
                                                                                callback.onSuccess(
                                                                                        allOrders
                                                                                )
                                                                            }
                                                                            override fun onError(
                                                                                    errorMessage:
                                                                                            String
                                                                            ) {
                                                                                Log.w(
                                                                                        "ApiManager",
                                                                                        "⚠️ Errore storico ordini, uso solo ordine corrente: $errorMessage"
                                                                                )
                                                                                val singleOrder =
                                                                                        JSONArray()
                                                                                                .apply {
                                                                                                    put(
                                                                                                            currentOrder
                                                                                                    )
                                                                                                }
                                                                                callback.onSuccess(
                                                                                        singleOrder
                                                                                )
                                                                            }
                                                                        }
                                                                )
                                                            }
                                                            override fun onError(
                                                                    errorMessage: String
                                                            ) {
                                                                Log.w(
                                                                        "ApiManager",
                                                                        "❌ Errore menu ordine corrente: $errorMessage"
                                                                )
                                                                callback.onError(
                                                                        "Errore recupero dettagli menu ordine corrente (mid=$currentMid): $errorMessage"
                                                                )
                                                            }
                                                        }
                                                )
                                            }
                                            override fun onError(errorMessage: String) {
                                                Log.w(
                                                        "ApiManager",
                                                        "⚠️ Errore recupero dettagli ordine corrente: $errorMessage. Procedo solo con ordini storici."
                                                )
                                                getCompletedOrders(uid, sid, callback)
                                            }
                                        }
                                )
                            } else {
                                Log.d(
                                        "ApiManager",
                                        "📭 Ordine non attivo (status: $orderStatus), recupero solo storico"
                                )
                                getCompletedOrders(uid, sid, callback)
                            }
                        } else {
                            Log.d(
                                    "ApiManager",
                                    "📭 Nessun ordine corrente trovato, recupero solo storico"
                            )
                            getCompletedOrders(uid, sid, callback)
                        }
                    }
                    override fun onError(errorMessage: String) {
                        Log.w(
                                "ApiManager",
                                "⚠️ Errore recupero utente, uso solo ordini storici: $errorMessage"
                        )
                        getCompletedOrders(uid, sid, callback)
                    }
                }
        )
    }

    /** Converte coordinate lat/lng in indirizzo usando Google Geocoding API */
    fun reverseGeocode(lat: Double, lng: Double, callback: (String?) -> Unit) {
        Thread {
                    try {
                        val apiKey = "AIzaSyCy8piB2kgN9bgqun4aMhkp7OXcfJTgeJE"
                        val url =
                                "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey"
                        Log.d("ApiManager", "🌍 Geocoding: $url")

                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string()

                        Log.d("ApiManager", "📍 Response code: ${response.code}")
                        Log.d("ApiManager", "📍 Response body: $responseBody")

                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val results = jsonResponse.getJSONArray("results")

                            if (results.length() > 0) {
                                val address =
                                        results.getJSONObject(0).getString("formatted_address")
                                Log.d("ApiManager", "✅ Indirizzo trovato: $address")
                                callback(address)
                            } else {
                                Log.w("ApiManager", "⚠️ Nessun risultato dal geocoding")
                                callback(null)
                            }
                        } else {
                            Log.e("ApiManager", "❌ Geocoding fallito: ${response.code}")
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("ApiManager", "💥 Errore geocoding: ${e.message}")
                        callback(null)
                    }
                }
                .start()
    }
}
