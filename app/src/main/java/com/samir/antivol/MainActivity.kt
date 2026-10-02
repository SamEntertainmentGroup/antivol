package com.samir.antivol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val et = EditText(this).apply {
            hint = "Numéro à appeler"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(prefs.getString("num", ""))
        }

        val titre = TextView(this).apply {
            text = "Sensibilité"
            textSize = 18f
            setPadding(0, 48, 0, 0)
        }
        val choix = listOf("Forte" to 1.2f, "Moyenne" to 2.0f, "Faible" to 3.5f)
        val actuel = prefs.getFloat("seuil", 2.0f)
        val rg = RadioGroup(this)
        choix.forEachIndexed { i, (nom, valeur) ->
            rg.addView(RadioButton(this).apply {
                id = i + 1
                text = nom
                isChecked = valeur == actuel
            })
        }

        val on = Button(this).apply { text = "Activer (armé dans 30 s)" }
        val off = Button(this).apply { text = "Arrêter" }

        on.setOnClickListener {
            val num = et.text.toString().trim()
            if (num.length < 8) { toast("Numéro invalide"); return@setOnClickListener }
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CALL_PHONE,
                    Manifest.permission.POST_NOTIFICATIONS), 1)
                toast("Autorise puis appuie à nouveau sur Activer")
                return@setOnClickListener
            }
            val idx = (rg.checkedRadioButtonId - 1).coerceIn(0, choix.size - 1)
            prefs.edit()
                .putString("num", num)
                .putFloat("seuil", choix[idx].second)
                .apply()
            stopService(Intent(this, MotionService::class.java))
            startForegroundService(Intent(this, MotionService::class.java))
            toast("Surveillance activée (${choix[idx].first})")
        }
        off.setOnClickListener {
            stopService(Intent(this, MotionService::class.java))
            toast("Arrêtée")
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(et); addView(titre); addView(rg); addView(on); addView(off)
        })
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
