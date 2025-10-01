package com.example.kotlin_food_delivery

/** Semplificato: definisce solo gli stati UI e l'interfaccia essenziali */
sealed class UIState {
    object Loading : UIState()
    object Empty : UIState()
    data class Success(val data: Any? = null) : UIState()
    data class Error(val message: String, val canRetry: Boolean = true) : UIState()
    data class Progress(val currentStep: Int, val totalSteps: Int, val message: String) : UIState()
}

/** Interfaccia per i fragment che gestiscono stati di caricamento */
interface StateAware {
    fun updateUIState(state: UIState)
}
