package com.example.kotlin_food_delivery

import kotlin.math.ceil

/**
 * Utility per la formattazione dei dati dell'app. Contiene funzioni comuni per il formatting di
 * tempi, prezzi, date, ecc.
 */
object FormattingUtils {

    /**
     * Formatta il tempo di consegna con logica realistica di arrotondamento:
     * - Minimo 30 secondi
     * - Arrotondamento per eccesso: 35s = 1min, 1:10 = 1:30, ecc.
     * - Incrementi di 30s per i primi 2 minuti, poi per minuti interi
     */
    fun formatDeliveryTime(deliveryTimeMinutes: Int): String {
        // Se è 0 o negativo, forza a 30 secondi
        if (deliveryTimeMinutes <= 0) {
            return "30 sec"
        }

        // Converti in secondi per lavorare meglio
        val totalSeconds = deliveryTimeMinutes * 60

        return when {
            // Da 0 a 30 secondi -> 30 sec
            totalSeconds <= 30 -> "30 sec"

            // Da 31 a 60 secondi -> 1 min
            totalSeconds <= 60 -> "1 min"

            // Da 61 a 90 secondi -> 1:30 min
            totalSeconds <= 90 -> "1:30 min"

            // Da 91 a 120 secondi -> 2 min
            totalSeconds <= 120 -> "2 min"

            // Oltre 2 minuti, arrotonda per eccesso al minuto intero
            else -> {
                val minutes = ceil(totalSeconds / 60.0).toInt()
                "$minutes min"
            }
        }
    }

    /** Formatta il prezzo con formato europeo (€12,50) */
    fun formatPrice(price: Double): String {
        return "€${String.format("%.2f", price).replace('.', ',')}"
    }

    /** Formatta il prezzo in versione compatta per le card (€12.50) */
    fun formatPriceCompact(price: Double): String {
        return "€${String.format("%.2f", price)}"
    }

    /** Formatta coordinate geografiche per display */
    fun formatCoordinates(lat: Double, lng: Double): String {
        return "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
    }

    /** Tronca una stringa alla lunghezza specificata aggiungendo "..." */
    fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            "${text.substring(0, maxLength - 3)}..."
        }
    }

    /** Capitalizza la prima lettera di ogni parola */
    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
    }
}
