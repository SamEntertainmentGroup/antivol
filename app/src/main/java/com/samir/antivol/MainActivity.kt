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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var banner: TextView
    private lateinit var statusCard: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var phoneInput: EditText
    private lateinit var sensitivityGroup: RadioGroup
    private lateinit var configCard: LinearLayout
    private lateinit var callBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var batBtn: Button

    private val handler = Handler(Looper.getMainLooper())
    private val hideBanner = Runnable { banner.visibility = View.GONE }

    private val choices = listOf(
        "Normal" to 0.8f,
        "Sensible" to 0.5f,
        "Extrême" to 0.25f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(20), dp(28), dp(20), dp(20))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        // BANDEAU DE MESSAGE
        banner = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(16), dp(18), dp(16))
            elevation = dp(8).toFloat()
            visibility = View.GONE
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
        statusCard = createCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }

        val statusDot = TextView(this).apply {
            text = "●"
            textSize = 18f
            setPadding(0, 0, dp(10), 0)
        }

        statusText = TextView(this).apply {
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

        statusCard.addView(
            statusDot,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        statusCard.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(statusCard)

        // CONFIGURATION
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
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                1
            )
        }

        notifBtn = addRow(configCard, "Autorisation de notification") {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                2
            )
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
            filters = arrayOf(InputFilter.LengthFilter(10))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setText(prefs.getString("num", ""))
            background = roundedBackground(
                Color.WHITE,
                Color.rgb(220, 223, 229),
                12
            )
        }

        phoneCard.addView(phoneInput)
        root.addView(phoneCard)

        // SENSIBILITÉ
        root.addView(createSectionTitle("Sensibilité"))

        val sensitivityCard = createCard()

        sensitivityGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val currentMode = prefs
            .getInt("mode", 0)
            .coerceIn(0, 2)

        choices.forEachIndexed { index, choice ->

            sensitivityGroup.addView(
                RadioButton(this).apply {
                    id = 100 + index
                    text = choice.first
                    textSize = 16f
                    setTextColor(Color.rgb(35, 38, 45))
                    setPadding(dp(4), dp(10), 0, dp(10))
                    isChecked = index == currentMode
                }
            )

            if (index < choices.lastIndex) {
                sensitivityGroup.addView(
                    View(this).apply {
                        setBackgroundColor(Color.rgb(235, 236, 240))
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }
        }

        sensitivityCard.addView(sensitivityGroup)
        root.addView(sensitivityCard)

        // BOUTONS NORMAUX
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(22), dp(4), dp(20))
        }

        val activateButton = Button(this).apply {
            text = "Activer la protection"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(25, 145, 80),
                Color.TRANSPARENT,
                12
            )
            stateListAnimator = null
        }

        val stopButton = Button(this).apply {
            text = "Arrêter la protection"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(200, 50, 60),
                Color.TRANSPARENT,
                12
            )
            stateListAnimator = null
        }

        buttons.addView(
            activateButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        buttons.addView(
            stopButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(buttons)

        updateStatus(prefs.getBoolean("active", false))
        refreshConfig()

        // ACTIVER
        activateButton.setOnClickListener {

            hideKeyboard()

            val num = phoneInput.text.toString().trim()

            if (!num.matches(Regex("^\\d{10}$"))) {
                showBanner(
                    "Numéro invalide : 10 chiffres requis",
                    false
                )
                return@setOnClickListener
            }

            if (!allOk()) {
                refreshConfig()
                showBanner(
                    "Termine d'abord la configuration",
                    false
                )
                scroll.smoothScrollTo(0, 0)
                return@setOnClickListener
            }

            val index = (
                sensitivityGroup.checkedRadioButtonId - 100
            ).coerceIn(0, 2)

            prefs.edit()
                .putString("num", num)
                .putInt("mode", index)
                .putFloat("seuil", choices[index].second)
                .putBoolean("active", true)
                .apply()

            stopService(
                Intent(this, MotionService::class.java)
            )

            startForegroundService(
                Intent(this, MotionService::class.java)
            )

            updateStatus(true)

            showBanner(
                "Protection activée",
                true
            )
        }

        // ARRÊTER
        stopButton.setOnClickListener {

            hideKeyboard()

            stopService(
                Intent(this, MotionService::class.java)
            )

            prefs.edit()
                .putBoolean("active", false)
                .apply()

            updateStatus(false)

            showBanner(
                "Protection arrêtée",
                true
            )
        }

        // ÉCRAN = contenu + bandeau par-dessus
        val frame = FrameLayout(this)

        frame.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        frame.addView(
            banner,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                setMargins(
                    dp(16),
                    dp(16),
                    dp(16),
                    0
                )
            }
        )

        setContentView(frame)
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideBanner)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        refreshConfig()

        if (
            grantResults.isNotEmpty() &&
            grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            showBanner(
                "Autorise dans les réglages de l'appli",
                false
            )

            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    // BANDEAU
    private fun showBanner(
        message: String,
        success: Boolean
    ) {
        banner.text = message

        banner.background = roundedBackground(
            if (success) {
                Color.rgb(25, 145, 80)
            } else {
                Color.rgb(200, 50, 60)
            },
            Color.TRANSPARENT,
            14
        )

        banner.visibility = View.VISIBLE

        handler.removeCallbacks(hideBanner)

        handler.postDelayed(
            hideBanner,
            3000
        )
    }

    // CLAVIER
    private fun hideKeyboard() {

        (
            getSystemService(INPUT_METHOD_SERVICE)
                as InputMethodManager
        ).hideSoftInputFromWindow(
            phoneInput.windowToken,
            0
        )

        phoneInput.clearFocus()

        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    // ÉTAT DES AUTORISATIONS
    private fun callOk() =
        checkSelfPermission(
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

    private fun notifOk() =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun batteryOk(): Boolean {

        val pm =
            getSystemService(POWER_SERVICE)
                as PowerManager

        return pm.isIgnoringBatteryOptimizations(
            packageName
        )
    }

    private fun allOk() =
        callOk() &&
        notifOk() &&
        batteryOk()

    private fun refreshConfig() {

        setBtn(callBtn, callOk())
        setBtn(notifBtn, notifOk())
        setBtn(batBtn, batteryOk())

        configCard.visibility =
            if (allOk()) {
                View.GONE
            } else {
                View.VISIBLE
            }
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
                startActivity(
                    Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    // OUTILS D'INTERFACE
    private fun addRow(
        card: LinearLayout,
        label: String,
        onClick: () -> Unit
    ): Button {

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
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val btn = Button(this).apply {
            isAllCaps = false
            textSize = 13f
            setOnClickListener {
                onClick()
            }
        }

        row.addView(
            btn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
            )
        )

        card.addView(row)

        return btn
    }

    private fun setBtn(
        btn: Button,
        ok: Boolean
    ) {

        btn.isEnabled = !ok

        btn.text = if (ok) {
            "✓ OK"
        } else {
            "Autoriser"
        }

        btn.setTextColor(Color.WHITE)

        btn.background = roundedBackground(
            if (ok) {
                Color.rgb(25, 145, 80)
            } else {
                Color.rgb(230, 126, 0)
            },
            Color.TRANSPARENT,
            12
        )
    }

    private fun createCard(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
            )

            background = roundedBackground(
                Color.WHITE,
                Color.rgb(232, 234, 238),
                16
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        0,
                        dp(6),
                        0,
                        dp(8)
                    )
                }
        }
    }

    private fun createSectionTitle(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text = text

            textSize = 13f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                Color.rgb(90, 95, 105)
            )

            setPadding(
                dp(4),
                dp(18),
                dp(4),
                dp(5)
            )
        }
    }

    private fun roundedBackground(
        color: Int,
        strokeColor: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()

            if (strokeColor != Color.TRANSPARENT) {
                setStroke(
                    dp(1),
                    strokeColor
                )
            }
        }
    }

    // STATUT
    private fun updateStatus(
        active: Boolean
    ) {

        val color =
            if (active) {
                Color.rgb(25, 145, 80)
            } else {
                Color.rgb(200, 50, 60)
            }

        statusText.text =
            if (active) {
                "Protection active"
            } else {
                "Protection non active"
            }

        statusText.setTextColor(color)

        val statusDot =
            statusCard.getChildAt(0) as TextView

        statusDot.setTextColor(color)

        statusCard.background =
            roundedBackground(
                Color.WHITE,
                color,
                16
            )
    }

    private fun dp(value: Int): Int =
        (
            value *
                resources.displayMetrics.density
        ).roundToInt()
}
