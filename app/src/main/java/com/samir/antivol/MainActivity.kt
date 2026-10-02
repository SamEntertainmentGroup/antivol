package com.samir.antivol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

    private val choices = listOf(
        "Normal" to 0.8f,
        "Sensible" to 0.5f,
        "Extrême" to 0.25f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        // =========================================================
        // FOND
        // =========================================================

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        // =========================================================
        // HEADER
        // =========================================================

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(24),
                dp(28),
                dp(24),
                dp(24)
            )
            setBackgroundColor(Color.rgb(25, 28, 35))
        }

        val appName = TextView(this).apply {
            text = "ANTIVOL"
            textSize = 13f
            setTextColor(Color.rgb(150, 155, 165))
            letterSpacing = 0.15f
        }

        val title = TextView(this).apply {
            text = "Protection antivol"
            textSize = 27f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(
                0,
                dp(8),
                0,
                dp(4)
            )
        }

        val subtitle = TextView(this).apply {
            text = "Protège votre téléphone contre les mouvements suspects."
            textSize = 14f
            setTextColor(Color.rgb(190, 194, 202))
        }

        header.addView(appName)
        header.addView(title)
        header.addView(subtitle)

        root.addView(header)

        // =========================================================
        // STATUT
        // =========================================================

        val statusCard = createCard()

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        statusDot = TextView(this).apply {
            text = "●"
            textSize = 22f
            setPadding(
                0,
                0,
                dp(12),
                0
            )
        }

        val statusColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusText = TextView(this).apply {
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
        }

        val statusDescription = TextView(this).apply {
            text = "La protection antivol est actuellement désactivée."
            textSize = 13f
            setTextColor(Color.rgb(110, 115, 125))
            setPadding(
                0,
                dp(3),
                0,
                0
            )
        }

        statusColumn.addView(statusText)
        statusColumn.addView(statusDescription)

        statusRow.addView(statusDot)
        statusRow.addView(statusColumn)

        statusCard.addView(statusRow)

        root.addView(statusCard)

        // =========================================================
        // NUMÉRO D'ALERTE
        // =========================================================

        root.addView(
            createSectionTitle("Numéro d'alerte")
        )

        val phoneCard = createCard()

        phoneInput = EditText(this).apply {
            hint = "0606060606"
            textSize = 16f

            inputType = InputType.TYPE_CLASS_PHONE

            setSingleLine(true)

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )

            setText(
                prefs.getString("num", "")
            )

            background = roundedBackground(
                Color.WHITE,
                Color.rgb(220, 223, 229),
                12
            )
        }

        phoneCard.addView(phoneInput)

        val phoneInfo = TextView(this).apply {
            text = "Le numéro doit contenir exactement 10 chiffres."
            textSize = 12f
            setTextColor(Color.rgb(115, 120, 130))

            setPadding(
                dp(2),
                dp(8),
                dp(2),
                0
            )
        }

        phoneCard.addView(phoneInfo)

        root.addView(phoneCard)

        // =========================================================
        // SENSIBILITÉ
        // =========================================================

        root.addView(
            createSectionTitle("Sensibilité")
        )

        val sensitivityCard = createCard()

        sensitivityGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val currentMode =
            prefs.getInt("mode", 0)
                .coerceIn(0, 2)

        choices.forEachIndexed { index, choice ->

            val radio = RadioButton(this).apply {

                id = 100 + index

                text = choice.first

                textSize = 16f

                setTextColor(
                    Color.rgb(35, 38, 45)
                )

                setPadding(
                    dp(4),
                    dp(10),
                    0,
                    dp(10)
                )

                isChecked =
                    index == currentMode
            }

            sensitivityGroup.addView(radio)

            if (index < choices.lastIndex) {

                val separator = View(this).apply {
                    setBackgroundColor(
                        Color.rgb(235, 236, 240)
                    )
                }

                sensitivityGroup.addView(
                    separator,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }
        }

        sensitivityCard.addView(
            sensitivityGroup
        )

        root.addView(sensitivityCard)

        // =========================================================
        // BOUTONS
        // =========================================================

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(22),
                dp(24),
                dp(30)
            )
        }

        val activateButton = Button(this).apply {

            text = "ACTIVER LA PROTECTION"

            textSize = 14f

            setTypeface(
                null,
                Typeface.BOLD
            )

            isAllCaps = false

            setTextColor(Color.WHITE)

            background = roundedBackground(
                Color.rgb(28, 125, 76),
                Color.TRANSPARENT,
                14
            )

            minimumHeight = dp(54)
        }

        val stopButton = Button(this).apply {

            text = "Arrêter la protection"

            textSize = 14f

            isAllCaps = false

            setTextColor(
                Color.rgb(190, 55, 55)
            )

            background = roundedBackground(
                Color.rgb(255, 242, 242),
                Color.TRANSPARENT,
                14
            )

            minimumHeight = dp(50)
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
                dp(52)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(buttons)

        // =========================================================
        // ÉTAT INITIAL
        // =========================================================

        updateStatus(
            prefs.getBoolean("active", false)
        )

        // =========================================================
        // ACTIVER
        // =========================================================

        activateButton.setOnClickListener {

            val num =
                phoneInput.text
                    .toString()
                    .trim()

            // Exactement 10 chiffres
            if (!num.matches(
                    Regex("^\\d{10}$")
                )
            ) {

                toast(
                    "Le numéro doit contenir exactement 10 chiffres"
                )

                phoneInput.requestFocus()

                return@setOnClickListener
            }

            // Permission appel
            if (
                checkSelfPermission(
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1
                )

                toast(
                    "Autorise les permissions puis appuie à nouveau sur Activer"
                )

                return@setOnClickListener
            }

            val selectedId =
                sensitivityGroup.checkedRadioButtonId

            val index = when (selectedId) {
                100 -> 0
                101 -> 1
                102 -> 2
                else -> 0
            }

            val selected =
                choices[index]

            // Sauvegarde
            prefs.edit()
                .putString("num", num)
                .putInt("mode", index)
                .putFloat("seuil", selected.second)
                .putBoolean("active", true)
                .apply()

            // Évite plusieurs services
            stopService(
                Intent(
                    this,
                    MotionService::class.java
                )
            )

            // Lance la surveillance
            startForegroundService(
                Intent(
                    this,
                    MotionService::class.java
                )
            )

            updateStatus(true)

            toast(
                "Protection activée — armement dans 30 secondes"
            )
        }

        // =========================================================
        // ARRÊTER
        // =========================================================

        stopButton.setOnClickListener {

            stopService(
                Intent(
                    this,
                    MotionService::class.java
                )
            )

            prefs.edit()
                .putBoolean("active", false)
                .apply()

            updateStatus(false)

            toast(
                "Protection antivol arrêtée"
            )
        }

        setContentView(scroll)
    }

    // =============================================================
    // CRÉATION D'UNE CARTE
    // =============================================================

    private fun createCard(): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
            )

            background =
                roundedBackground(
                    Color.WHITE,
                    Color.rgb(232, 234, 238),
                    16
                )

            val params =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            params.setMargins(
                dp(20),
                dp(8),
                dp(20),
                dp(8)
            )

            layoutParams = params
        }
    }

    // =============================================================
    // TITRE DE SECTION
    // =============================================================

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
                dp(24),
                dp(18),
                dp(24),
                dp(4)
            )
        }
    }

    // =============================================================
    // BACKGROUND ARRONDI
    // =============================================================

    private fun roundedBackground(
        color: Int,
        strokeColor: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()

            if (
                strokeColor !=
                Color.TRANSPARENT
            ) {

                setStroke(
                    dp(1),
                    strokeColor
                )
            }
        }
    }

    // =============================================================
    // STATUT
    // =============================================================

    private fun updateStatus(
        active: Boolean
    ) {

        if (active) {

            statusText.text =
                "Protection active"

            statusText.setTextColor(
                Color.rgb(28, 125, 76)
            )

            statusDot.setTextColor(
                Color.rgb(28, 160, 90)
            )

        } else {

            statusText.text =
                "Protection inactive"

            statusText.setTextColor(
                Color.rgb(80, 84, 92)
            )

            statusDot.setTextColor(
                Color.rgb(150, 155, 165)
            )
        }
    }

    // =============================================================
    // DP
    // =============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    // =============================================================
    // TOAST
    // =============================================================

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
 
