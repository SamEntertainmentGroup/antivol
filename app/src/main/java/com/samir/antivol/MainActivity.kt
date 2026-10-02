package com.samir.antivol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var statusDot: TextView
    private lateinit var phoneInput: EditText
    private lateinit var sensitivityGroup: RadioGroup
    private lateinit var configCard: LinearLayout
    private lateinit var callBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var batBtn: Button

    private val choices = listOf(
        "Normal" to 0.8f,
        "Sensible" to 0.5f,
        "Extrême" to 0.25f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(20), dp(28), dp(20), dp(20))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        // TITRE
        root.addView(TextView(this).apply {
            text = "Protection antivol"
            textSize = 29f
            setTextColor(Color.rgb(25, 28, 35))
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(4), dp(4), dp(4), dp(18))
        })

        // STATUT
        val statusCard = createCard()
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusDot = TextView(this).apply {
            text = "●"
            textSize = 21f
            setPadding(0, 0, dp(12), 0)
        }
        statusText = TextView(this).apply {
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
        }
        statusRow.addView(statusDot)
        statusRow.addView(statusText)
        statusCard.addView(statusRow)
        root.addView(statusCard)

        // CONFIGURATION (disparaît quand tout est vert)
        configCard = createCard().apply {
            background = roundedBackground(
                Color.rgb(255, 243, 224),
                Color.rgb(255, 204, 128),
                16
            )
        }
        configCard.addView(TextView(this).apply {
            text = "⚠ Configuration requise"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(160, 90, 0))
            setPadding(0, 0, 0, dp(6))
        })
        callBtn = addRow(configCard, "Autorisation d'appel") {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
        notifBtn = addRow(configCard, "Autorisation de notification") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
        batBtn = addRow(configCard, "Batterie sans restriction") {
            askBattery()
        }
        root.addView(configCard)

        // NUMÉRO
        root.addView(createSectionTitle("Numéro d'alerte"))
        val phoneCard = createCard()
        phoneInput = EditText(this).apply {
            hint = "0606060606"
            textSize = 17f
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setText(prefs.getString("num", ""))
            background = roundedBackground(Color.WHITE, Color.rgb(220, 223, 229), 12)
        }
        phoneCard.addView(phoneInput)
        root.addView(phoneCard)

        // SENSIBILITÉ
        root.addView(createSectionTitle("Sensibilité"))
        val sensitivityCard = createCard()
        sensitivityGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val currentMode = prefs.getInt("mode", 0).coerceIn(0, 2)
        choices.forEachIndexed { index, choice ->
            sensitivityGroup.addView(RadioButton(this).apply {
                id = 100 + index
                text = choice.first
                textSize = 16f
                setTextColor(Color.rgb(35, 38, 45))
                setPadding(dp(4), dp(10), 0, dp(10))
                isChecked = index == currentMode
            })
            if (index < choices.lastIndex) {
                sensitivityGroup.addView(
                    View(this).apply { setBackgroundColor(Color.rgb(235, 236, 240)) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                )
            }
        }
        sensitivityCard.addView(sensitivityGroup)
        root.addView(sensitivityCard)

        // BOUTONS
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(22), dp(4), dp(20))
        }
        val activateButton = Button(this).apply {
            text = "Activer la protection"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(25, 145, 80), Color.TRANSPARENT, 14)
            minimumHeight = dp(56)
        }
        val stopButton = Button(this).apply {
            text = "Arrêter la protection"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            setTextColor(Color.rgb(190, 45, 55))
            background = roundedBackground(
                Color.rgb(255, 235, 237),
                Color.rgb(245, 205, 209),
                14
            )
            minimumHeight = dp(54)
        }
        buttons.addView(
            activateButton,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        )
        buttons.addView(
            stopButton,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
                .apply { topMargin = dp(12) }
        )
        root.addView(buttons)

        updateStatus(prefs.getBoolean("active", false))
        refreshConfig()

        // ACTIVER
        activateButton.setOnClickListener {
            val num = phoneInput.text.toString().trim()
            if (!num.matches(Regex("^\\d{10}$"))) {
                toast("Numéro invalide (10 chiffres)")
                phoneInput.requestFocus()
                return@setOnClickListener
            }

            if (!allOk()) {
                refreshConfig()
                toast("Termine d'abord la configuration en haut")
                scroll.smoothScrollTo(0, 0)
                return@setOnClickListener
            }

            val index = (sensitivityGroup.checkedRadioButtonId - 100).coerceIn(0, 2)
            prefs.edit()
                .putString("num", num)
                .putInt("mode", index)
                .putFloat("seuil", choices[index].second)
                .putBoolean("active", true)
                .apply()

            stopService(Intent(this, MotionService::class.java))
            startForegroundService(Intent(this, MotionService::class.java))
            updateStatus(true)
            toast("Protection activée")
        }

        // ARRÊTER
        stopButton.setOnClickListener {
            stopService(Intent(this, MotionService::class.java))
            prefs.edit().putBoolean("active", false).apply()
            updateStatus(false)
            toast("Protection arrêtée")
        }

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshConfig()
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            toast("Autorise dans les réglages de l'appli")
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    // ÉTAT DES AUTORISATIONS
    private fun callOk() =
        checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun notifOk() =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun batteryOk(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun allOk() = callOk() && notifOk() && batteryOk()

    private fun refreshConfig() {
        setBtn(callBtn, callOk())
        setBtn(notifBtn, notifOk())
        setBtn(batBtn, batteryOk())
        configCard.visibility = if (allOk()) View.GONE else View.VISIBLE
    }

    private fun askBattery() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }

    // OUTILS D'INTERFACE
    private fun addRow(card: LinearLayout, label: String, onClick: () -> Unit): Button {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(
            TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.rgb(35, 38, 45))
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        val btn = Button(this).apply {
            isAllCaps = false
            textSize = 13f
            setOnClickListener { onClick() }
        }
        row.addView(
            btn,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44))
        )
        card.addView(row)
        return btn
    }

    private fun setBtn(btn: Button, ok: Boolean) {
        btn.isEnabled = !ok
        btn.text = if (ok) "✓ OK" else "Autoriser"
        btn.setTextColor(Color.WHITE)
        btn.background = roundedBackground(
            if (ok) Color.rgb(25, 145, 80) else Color.rgb(230, 126, 0),
            Color.TRANSPARENT,
            12
        )
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBackground(Color.WHITE, Color.rgb(232, 234, 238), 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(6), 0, dp(8)) }
        }
    }

    private fun createSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(90, 95, 105))
            setPadding(dp(4), dp(18), dp(4), dp(5))
        }
    }

    private fun roundedBackground(color: Int, strokeColor: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
        }
    }

    private fun updateStatus(active: Boolean) {
        if (active) {
            statusText.text = "Protection active"
            statusText.setTextColor(Color.rgb(25, 145, 80))
            statusDot.setTextColor(Color.rgb(25, 165, 85))
        } else {
            statusText.text = "Protection non active"
            statusText.setTextColor(Color.rgb(90, 94, 102))
            statusDot.setTextColor(Color.rgb(150, 155, 165))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
