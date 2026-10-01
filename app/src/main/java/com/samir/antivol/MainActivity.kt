package com.samir.antivol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val number = EditText(this).apply {
            hint = "Numéro à appeler"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(prefs.getString("num", ""))
        }

        val activate = Button(this).apply {
            text = "Activer (armé dans 30 s)"
        }

        val stop = Button(this).apply {
            text = "Arrêter"
        }

        activate.setOnClickListener {
            val num = number.text.toString().trim()

            if (num.length < 8) {
                toast("Numéro invalide")
                return@setOnClickListener
            }

            if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1
                )

                toast("Autorise les permissions puis appuie à nouveau sur Activer")
                return@setOnClickListener
            }

            prefs.edit().putString("num", num).apply()

            startForegroundService(
                Intent(this, MotionService::class.java)
            )

            toast("Surveillance activée dans 30 s")
        }

        stop.setOnClickListener {
            stopService(Intent(this, MotionService::class.java))
            toast("Surveillance arrêtée")
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 96, 48, 48)

                addView(number)
                addView(activate)
                addView(stop)
            }
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
