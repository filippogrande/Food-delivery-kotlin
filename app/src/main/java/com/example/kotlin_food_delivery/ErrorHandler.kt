package com.example.kotlin_food_delivery

import android.util.Log

/**
 * Gestore centralizzato per gli errori, ispirato ai pattern di error handling del React Native.
 * Fornisce logging consistente e messaggi di errore user-friendly.
 */
object ErrorHandler {

    enum class ErrorType {
        NETWORK,
        AUTHENTICATION,
        PROFILE,
        ORDER,
        MENU,
        LOCATION,
        VALIDATION,
        UNKNOWN
    }

    data class AppError(
            val type: ErrorType,
            val message: String,
            val technicalDetails: String? = null,
            val userMessage: String? = null,
            val canRetry: Boolean = false
    )

    /** Crea un errore di rete con messaggio user-friendly */
    fun createNetworkError(technicalMessage: String, endpoint: String? = null): AppError {
        val userMessage =
                when {
                    technicalMessage.contains("404") ->
                            "Servizio non disponibile al momento. Riprova più tardi."
                    technicalMessage.contains("timeout", ignoreCase = true) ->
                            "Connessione lenta. Controlla la tua connessione internet."
                    technicalMessage.contains("connection", ignoreCase = true) ->
                            "Problema di connessione. Verifica di essere connesso a internet."
                    else -> "Errore di connessione. Riprova più tardi."
                }

        return AppError(
                type = ErrorType.NETWORK,
                message = technicalMessage,
                technicalDetails = endpoint?.let { "Endpoint: $it" },
                userMessage = userMessage,
                canRetry = true
        )
    }

    /** Crea un errore di profilo */
    fun createProfileError(technicalMessage: String): AppError {
        val userMessage =
                when {
                    technicalMessage.contains("incomplete", ignoreCase = true) ->
                            "Completa il tuo profilo prima di continuare."
                    technicalMessage.contains("authentication", ignoreCase = true) ->
                            "Sessione scaduta. Effettua nuovamente il login."
                    else -> "Errore nel caricamento del profilo. Riprova più tardi."
                }

        return AppError(
                type = ErrorType.PROFILE,
                message = technicalMessage,
                userMessage = userMessage,
                canRetry = true
        )
    }

    /** Crea un errore di ordine */
    fun createOrderError(technicalMessage: String): AppError {
        val userMessage =
                when {
                    technicalMessage.contains("not found", ignoreCase = true) ->
                            "Ordine non trovato."
                    technicalMessage.contains("cannot order", ignoreCase = true) ->
                            "Non puoi effettuare un nuovo ordine mentre uno è in consegna."
                    else -> "Errore nella gestione dell'ordine. Riprova più tardi."
                }

        return AppError(
                type = ErrorType.ORDER,
                message = technicalMessage,
                userMessage = userMessage,
                canRetry = false
        )
    }

    /** Crea un errore di menu */
    fun createMenuError(technicalMessage: String): AppError {
        return AppError(
                type = ErrorType.MENU,
                message = technicalMessage,
                userMessage =
                        "Errore nel caricamento del menu. I dati potrebbero non essere aggiornati.",
                canRetry = true
        )
    }

    /** Crea un errore di localizzazione */
    fun createLocationError(technicalMessage: String): AppError {
        val userMessage =
                when {
                    technicalMessage.contains("permission", ignoreCase = true) ->
                            "Abilita i permessi di localizzazione per procedere."
                    technicalMessage.contains("not available", ignoreCase = true) ->
                            "Servizio di localizzazione non disponibile."
                    else -> "Errore nella localizzazione. Controlla le impostazioni GPS."
                }

        return AppError(
                type = ErrorType.LOCATION,
                message = technicalMessage,
                userMessage = userMessage,
                canRetry = true
        )
    }

    /** Log un errore con livello appropriato */
    fun logError(error: AppError, tag: String = "ErrorHandler") {
        when (error.type) {
            ErrorType.NETWORK -> Log.w(tag, "🌐 Network Error: ${error.message}")
            ErrorType.AUTHENTICATION -> Log.w(tag, "🔐 Auth Error: ${error.message}")
            ErrorType.PROFILE -> Log.w(tag, "👤 Profile Error: ${error.message}")
            ErrorType.ORDER -> Log.e(tag, "📦 Order Error: ${error.message}")
            ErrorType.MENU -> Log.w(tag, "🍽️ Menu Error: ${error.message}")
            ErrorType.LOCATION -> Log.w(tag, "📍 Location Error: ${error.message}")
            ErrorType.VALIDATION -> Log.w(tag, "✅ Validation Error: ${error.message}")
            ErrorType.UNKNOWN -> Log.e(tag, "❓ Unknown Error: ${error.message}")
        }

        error.technicalDetails?.let { Log.d(tag, "Technical details: $it") }
    }

    /** Ottiene il messaggio da mostrare all'utente */
    fun getUserMessage(error: AppError): String {
        return error.userMessage ?: "Si è verificato un errore imprevisto. Riprova più tardi."
    }

    /** Verifica se l'errore permette un retry */
    fun canRetry(error: AppError): Boolean {
        return error.canRetry
    }
}
