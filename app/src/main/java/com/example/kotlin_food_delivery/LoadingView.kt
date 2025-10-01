package com.example.kotlin_food_delivery

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar

/**
 * Vista di caricamento semplice con rotellina colorata. Ispirata ai pattern di loading del React
 * Native con ActivityIndicator.
 */
class LoadingView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        LinearLayout(context, attrs, defStyleAttr) {

    private val progressBar: ProgressBar

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(48, 48, 48, 48)

        // ProgressBar principale - solo la rotellina colorata
        progressBar =
                ProgressBar(context).apply {
                    isIndeterminate = true
                    // Colore arancione come richiesto
                    indeterminateDrawable?.setTint(0xFFFC6444.toInt())
                }

        // Solo la rotellina, nessun testo
        addView(progressBar)
    }

    /** Animazione di fade in per un ingresso elegante */
    fun fadeIn(duration: Long = 300) {
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(duration).start()
    }

    /** Animazione di fade out per un'uscita elegante */
    fun fadeOut(duration: Long = 200, onComplete: (() -> Unit)? = null) {
        animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    visibility = View.GONE
                    onComplete?.invoke()
                }
                .start()
    }
}
