package com.example.kotlin_food_delivery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val layout =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }
        val nome = EditText(requireContext()).apply { hint = "Nome" }
        val cognome = EditText(requireContext()).apply { hint = "Cognome" }
        val nomeCarta = EditText(requireContext()).apply { hint = "Nome e Cognome (Carta)" }
        val numeroCarta =
                EditText(requireContext()).apply {
                    hint = "Numero carta"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
        val scadenzaMese =
                EditText(requireContext()).apply {
                    hint = "Mese scadenza (MM)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
        val scadenzaAnno =
                EditText(requireContext()).apply {
                    hint = "Anno scadenza (YY)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
        val codiceCVV =
                EditText(requireContext()).apply {
                    hint = "CVV"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }

        // Precompila i campi con ApiManager
        val prefs =
                requireContext()
                        .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
        val uid = prefs.getInt("uid", -1)
        val sid = prefs.getString("sid", "") ?: ""
        Log.d("ProfileFragment", "sid: $sid, uid: $uid")

        if (uid != -1) {
            ApiManager.getUser(
                    uid,
                    sid,
                    object : ApiManager.UserCallback {
                        override fun onSuccess(user: org.json.JSONObject) {
                            activity?.runOnUiThread {
                                nome.setText(user.optString("firstName", ""))
                                cognome.setText(user.optString("lastName", ""))
                                nomeCarta.setText(user.optString("cardFullName", ""))
                                numeroCarta.setText(user.optString("cardNumber", ""))
                                scadenzaMese.setText(user.optInt("cardExpireMonth", 0).toString())
                                scadenzaAnno.setText(user.optInt("cardExpireYear", 0).toString())
                                codiceCVV.setText(user.optString("cardCVV", ""))
                            }
                        }

                        override fun onError(errorMessage: String) {
                            Log.e("ProfileFragment", "Errore caricamento profilo: $errorMessage")
                        }
                    }
            )
        }

        val salva = Button(requireContext())
        salva.text = "Salva"

        salva.setOnClickListener {
            val firstName = nome.text.toString().trim()
            val lastName = cognome.text.toString().trim()
            val cardFullName = nomeCarta.text.toString().trim()
            val cardNumber = numeroCarta.text.toString().trim()
            val cardExpireMonth = scadenzaMese.text.toString().trim()
            val cardExpireYear = scadenzaAnno.text.toString().trim()
            val cardCVV = codiceCVV.text.toString().trim()

            // Validazione campi
            if (firstName.isEmpty() ||
                            lastName.isEmpty() ||
                            cardFullName.isEmpty() ||
                            cardNumber.isEmpty() ||
                            cardExpireMonth.isEmpty() ||
                            cardExpireYear.isEmpty() ||
                            cardCVV.isEmpty()
            ) {
                Toast.makeText(context, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cardNumber.length != 16) {
                Toast.makeText(
                                context,
                                "Il numero della carta deve essere di 16 cifre",
                                Toast.LENGTH_SHORT
                        )
                        .show()
                return@setOnClickListener
            }

            val prefs2 =
                    requireContext()
                            .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
            val sid2 = prefs2.getString("sid", "") ?: ""
            val uid2 = prefs2.getInt("uid", -1)
            if (sid2.isEmpty() || uid2 == -1) {
                Toast.makeText(context, "Sessione non valida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json =
                    org.json.JSONObject().apply {
                        put("firstName", firstName)
                        put("lastName", lastName)
                        put("cardFullName", cardFullName)
                        put("cardNumber", cardNumber)
                        put("cardExpireMonth", cardExpireMonth.toInt())
                        put("cardExpireYear", cardExpireYear.toInt())
                        put("cardCVV", cardCVV)
                        put("sid", sid2)
                    }

            ApiManager.updateUser(
                    uid2,
                    sid2,
                    json,
                    object : ApiManager.UserCallback {
                        override fun onSuccess(user: org.json.JSONObject) {
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Dati salvati!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onError(errorMessage: String) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                                context,
                                                "Errore salvataggio: $errorMessage",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    }
            )
        }

        layout.addView(nome)
        layout.addView(cognome)
        layout.addView(nomeCarta)
        layout.addView(numeroCarta)
        layout.addView(scadenzaMese)
        layout.addView(scadenzaAnno)
        layout.addView(codiceCVV)
        layout.addView(salva)

        return layout
    }
}
