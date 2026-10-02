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

        val label = TextView(this).apply {
            textSize = 18f
            setPadding(0, 48, 0, 0)
        }
        val bar = SeekBar(this).apply {
            max = 9
            progress = prefs.getInt("niveau", 7) - 1
        }
        fun maj() { label.text = "Sensibilité : ${bar.progress + 1} / 10" }
        maj()
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) = maj()
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

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
            val niveau = bar.progress + 1
            val seuil = 1.5f - niveau * 0.13f
            prefs.edit()
                .putString("num", num)
                .putInt("niveau", niveau)
                .putFloat("seuil", seuil)
                .putBoolean("ecran", cb.isChecked)
                .apply()
            stopService(Intent(this, MotionService::class.java))
            startForegroundService(Intent(this, MotionService::class.java))
            toast("Surveillance activée (sensibilité $niveau/10)")
        }
        off.setOnClickListener {
            stopService(Intent(this, MotionService::class.java))
            toast("Arrêtée")
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(et); addView(label); addView(bar); addView(cb); addView(on); addView(off)
        })
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
