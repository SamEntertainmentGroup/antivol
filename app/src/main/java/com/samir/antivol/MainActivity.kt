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
        val choix = listOf("Normal" to 0.8f, "Sensible" to 0.5f, "Extrême" to 0.25f)
        val rg = RadioGroup(this)
        val actuel = prefs.getInt("mode", 0).coerceIn(0, 2)
        choix.forEachIndexed { i, (nom, _) ->
            rg.addView(RadioButton(this).apply {
                id = i + 1
                text = nom
                isChecked = i == actuel
            })
        }

        val cb = CheckBox(this).apply {
            text = "Allumer l'écran à l'appel (test)"
            isChecked = prefs.getBoolean("ecran", false)
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
            val idx = (rg.checkedRadioButtonId - 1).coerceIn(0, 2)
            prefs.edit()
                .putString("num", num)
                .putInt("mode", idx)
                .putFloat("seuil", choix[idx].second)
                .putBoolean("ecran", cb.isChecked)
                .apply() 
