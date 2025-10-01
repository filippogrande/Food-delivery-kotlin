package com.example.kotlin_food_delivery

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationManager private constructor() {

    companion object {
        @Volatile private var INSTANCE: LocationManager? = null
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CACHE_DURATION = 10 * 60 * 1000L // 10 minuti

        fun getInstance(): LocationManager {
            return INSTANCE
                    ?: synchronized(this) { INSTANCE ?: LocationManager().also { INSTANCE = it } }
        }
    }

    // Cache della posizione
    private var cachedLat: Double? = null
    private var cachedLng: Double? = null
    private var locationCacheTime: Long = 0

    data class Location(val lat: Double, val lng: Double)

    fun getCurrentLocation(
            fragment: Fragment,
            callback: (Location?) -> Unit,
            showToasts: Boolean = true
    ) {
        val context = fragment.requireContext()

        // Controlla cache
        val currentTime = System.currentTimeMillis()
        if (cachedLat != null &&
                        cachedLng != null &&
                        (currentTime - locationCacheTime) < CACHE_DURATION
        ) {
            Log.d("LocationManager", "📍 Uso posizione dalla cache: $cachedLat, $cachedLng")
            callback(Location(cachedLat!!, cachedLng!!))
            return
        }

        Log.d("LocationManager", "🔄 Cache posizione scaduta o assente, richiedo nuova posizione")

        // Controlla permessi
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationUpdate(fragment, callback, showToasts)
        } else {
            // Richiedi permessi
            ActivityCompat.requestPermissions(
                    fragment.requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
            )
            // Usa posizione di fallback
            handleLocationFallback(fragment, callback, showToasts, "Permessi GPS non concessi")
        }
    }

    private fun requestLocationUpdate(
            fragment: Fragment,
            callback: (Location?) -> Unit,
            showToasts: Boolean
    ) {
        val context = fragment.requireContext()
        val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(fragment.requireActivity())

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            handleLocationFallback(fragment, callback, showToasts, "Permessi GPS mancanti")
            return
        }

        val locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                        .setMinUpdateIntervalMillis(2000)
                        .setMaxUpdates(1)
                        .build()

        val locationCallback =
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        val location = locationResult.lastLocation
                        if (location != null) {
                            Log.d(
                                    "LocationManager",
                                    "✅ Posizione GPS ottenuta: ${location.latitude}, ${location.longitude}"
                            )

                            // Salva in cache
                            cachedLat = location.latitude
                            cachedLng = location.longitude
                            locationCacheTime = System.currentTimeMillis()

                            callback(Location(location.latitude, location.longitude))
                        } else {
                            Log.w("LocationManager", "❌ LocationResult è null")
                            handleLocationFallback(
                                    fragment,
                                    callback,
                                    showToasts,
                                    "GPS non disponibile"
                            )
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

        // Timeout
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            Log.w("LocationManager", "⏰ Timeout GPS dopo 20 secondi")
            fusedLocationClient.removeLocationUpdates(locationCallback)
            handleLocationFallback(fragment, callback, showToasts, "Timeout GPS (20s)")
        }

        timeoutHandler.postDelayed(timeoutRunnable, 20000)

        try {
            Log.d("LocationManager", "Avvio requestLocationUpdates...")
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Errore sicurezza GPS: ${e.message}")
            timeoutHandler.removeCallbacks(timeoutRunnable)
            handleLocationFallback(fragment, callback, showToasts, "Errore permessi GPS")
        }
    }

    private fun handleLocationFallback(
            fragment: Fragment,
            callback: (Location?) -> Unit,
            showToasts: Boolean,
            reason: String
    ) {
        // Usa posizione cache se disponibile, altrimenti Milano
        val defaultLat = cachedLat ?: 45.4642
        val defaultLng = cachedLng ?: 9.1900

        val message =
                "$reason, uso ${if (cachedLat != null) "posizione precedente" else "Milano come default"}"

        if (showToasts) {
            fragment.context?.let { ctx -> Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show() }
        }

        Log.d("LocationManager", message)
        callback(Location(defaultLat, defaultLng))
    }

    // Funzione per gestire il risultato dei permessi
    fun handlePermissionResult(
            fragment: Fragment,
            requestCode: Int,
            grantResults: IntArray,
            callback: (Location?) -> Unit,
            showToasts: Boolean = true
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdate(fragment, callback, showToasts)
            } else {
                handleLocationFallback(fragment, callback, showToasts, "Permesso posizione negato")
            }
        }
    }

    fun getLocationPermissionRequestCode(): Int = LOCATION_PERMISSION_REQUEST_CODE
}
