
package com.autoalert.app

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
    private lateinit var scroll: ScrollView
    private lateinit var banner: TextView
    private lateinit var configCard: LinearLayout
    private lateinit var callBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var batBtn: Button
    private lateinit var phoneInput: EditText
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var sensitivityLabels: LinearLayout
    private lateinit var toggleButton: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val hideBanner = Runnable {
        banner.visibility = View.GONE
    }

    private val choiceNames = listOf(
        R.string.sens_normal,
        R.string.sens_sensible,
        R.string.sens_extreme
    )

    // Seuils d'origine : ne pas modifier le fonctionnement de MotionService.
    private val choiceValues = listOf(0.40f, 0.20f, 0.10f)

    private var updatingSlider = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(20), dp(18), dp(20), dp(20))
        }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        banner = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(14), dp(18), dp(14))
            elevation = dp(8).toFloat()
            visibility = View.GONE
        }

        // 1. Configuration : affichée uniquement si nécessaire.
        configCard = createCard().apply {
            background = roundedBackground(
                Color.rgb(255, 243, 224),
                Color.rgb(255, 204, 128),
                16
            )
        }

        configCard.addView(TextView(this).apply {
            text = getString(R.string.config_title)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(160, 90, 0))
            setPadding(0, 0, 0, dp(6))
        })

        callBtn = addRow(
            configCard,
            getString(R.string.perm_call)
        ) {
            val permissions = mutableListOf(
                Manifest.permission.CALL_PHONE
            )

            if (Build.VERSION.SDK_INT >= 26) {
                permissions.add(
                    Manifest.permission.ANSWER_PHONE_CALLS
                )
            }

            requestPermissions(
                permissions.toTypedArray(),
                1
            )
        }

        notifBtn = addRow(
            configCard,
            getString(R.string.perm_notif)
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2
                )
            } else {
                refreshConfig()
            }
        }

        batBtn = addRow(
            configCard,
            getString(R.string.perm_battery)
        ) {
            askBattery()
        }

        root.addView(configCard)

        // 2. Sensibilité : curseur horizontal à trois positions.
        root.addView(
            createSectionTitle(
                getString(R.string.section_sensitivity)
            )
        )

        val sensitivityCard = createCard()

        sensitivityLabels = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        choiceNames.forEachIndexed { index, labelRes ->
            sensitivityLabels.addView(
                TextView(this).apply {
                    text = getString(labelRes)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(35, 38, 45))
                    tag = index
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        sensitivityCard.addView(sensitivityLabels)

        sensitivitySlider = SeekBar(this).apply {
            max = 2
            progress = prefs.getInt("mode", 0).coerceIn(0, 2)
            contentDescription =
                getString(R.string.section_sensitivity)
        }

        sensitivityCard.addView(
            sensitivitySlider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        root.addView(sensitivityCard)

        sensitivitySlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (updatingSlider || !fromUser) return

                    val prefsNow =
                        getSharedPreferences("p", MODE_PRIVATE)

                    val active =
                        prefsNow.getBoolean("active", false)

                    // Impossible de modifier la sensibilité pendant
                    // que la protection est active.
                    if (active) {
                        setSensitivitySlider(
                            prefsNow.getInt("mode", 0)
                        )
                        return
                    }

                    // Les niveaux supplémentaires nécessitent
                    // un code de déblocage valide.
                    if (
                        progress > 0 &&
                        !License.isUnlocked(this@MainActivity)
                    ) {
                        setSensitivitySlider(0)
                        showBanner(
                            getString(R.string.msg_locked_option),
                            false
                        )
                        return
                    }

                    prefsNow.edit()
                        .putInt("mode", progress)
                        .putFloat(
                            "seuil",
                            choiceValues[progress]
                        )
                        .apply()

                    refreshSensitivityLabels(progress)
                }
            }
        )

        // 3. Grand cercle unique pour activer ou arrêter la protection.
        val circleHolder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        }

        toggleButton = TextView(this).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            isClickable = true
            isFocusable = true
            elevation = dp(4).toFloat()
        }

        circleHolder.addView(
            toggleButton,
            LinearLayout.LayoutParams(dp(220), dp(220))
        )

        root.addView(circleHolder)

        toggleButton.setOnClickListener {
            toggleProtection()
        }

        // 4. Numéro de téléphone sous le cercle.
        root.addView(
            createSectionTitle(
                getString(R.string.section_number)
            )
        )

        val phoneCard = createCard()

        phoneInput = EditText(this).apply {
            hint = getString(R.string.phone_hint)
            setHintTextColor(Color.rgb(160, 165, 175))
            textSize = 17f
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(20))
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

        // 5. Bouton Plus.
        val plusButton = Button(this).apply {
            text = getString(R.string.btn_plus)
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.rgb(35, 38, 45))
            background = roundedBackground(
                Color.WHITE,
                Color.rgb(35, 38, 45),
                12
            )
            stateListAnimator = null

            setOnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        PlusActivity::class.java
                    )
                )
            }
        }

        root.addView(
            plusButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(18)
            }
        )

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
                setMargins(dp(16), dp(24), dp(16), 0)
            }
        )

        setContentView(frame)

        refreshConfig()
        refreshSensitivityLabels(sensitivitySlider.progress)
        updateToggle(prefs.getBoolean("active", false))
    }

    private fun toggleProtection() {
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        // Si la protection est déjà active, le cercle l'arrête.
        if (prefs.getBoolean("active", false)) {
            hideKeyboard()

            stopService(
                Intent(this, MotionService::class.java)
            )

            prefs.edit()
                .putBoolean("active", false)
                .apply()

            updateToggle(false)

            showBanner(
                getString(R.string.msg_stopped),
                false
            )
            return
        }

        hideKeyboard()

        val num = phoneInput.text.toString()
            .replace(Regex("[\\s\\-().]"), "")

        if (!num.matches(Regex("^\\d{6,15}$"))) {
            showBanner(
                getString(R.string.msg_invalid_number),
                false
            )

            phoneInput.requestFocus()

            scroll.post {
                scroll.smoothScrollTo(0, phoneInput.top)
            }
            return
        }

        if (!allOk()) {
            refreshConfig()

            showBanner(
                getString(R.string.msg_finish_config),
                false
            )

            scroll.smoothScrollTo(0, 0)
            return
        }

        var index = sensitivitySlider.progress.coerceIn(0, 2)

        if (
            index > 0 &&
            !License.isUnlocked(this)
        ) {
            index = 0
        }

        setSensitivitySlider(index)

        prefs.edit()
            .putString("num", num)
            .putInt("mode", index)
            .putFloat("seuil", choiceValues[index])
            .putBoolean("active", true)
            .apply()

        try {
            stopService(
                Intent(this, MotionService::class.java)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(
                    Intent(this, MotionService::class.java)
                )
            } else {
                startService(
                    Intent(this, MotionService::class.java)
                )
            }

            updateToggle(true)

            showBanner(
                getString(R.string.msg_activated),
                true
            )
        } catch (_: Exception) {
            prefs.edit()
                .putBoolean("active", false)
                .apply()

            updateToggle(false)

            showBanner(
                getString(R.string.msg_start_error),
                false
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (::configCard.isInitialized) {
            refreshConfig()
        }

        if (::sensitivitySlider.isInitialized) {
            val prefs = getSharedPreferences("p", MODE_PRIVATE)
            val mode = prefs.getInt("mode", 0).coerceIn(0, 2)

            setSensitivitySlider(
                if (License.isUnlocked(this)) mode else 0
            )

            updateToggle(
                prefs.getBoolean("active", false)
            )
        }
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
            grantResults.any {
                it != PackageManager.PERMISSION_GRANTED
            }
        ) {
            showBanner(
                getString(R.string.msg_allow_settings),
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

    // Met à jour l'apparence du grand cercle et le verrouillage du curseur.
    private fun updateToggle(active: Boolean) {
        toggleButton.text = if (active) {
            "🔒\n" + getString(R.string.toggle_active)
        } else {
            "🔓\n" + getString(R.string.toggle_inactive)
        }

        toggleButton.background = roundedBackground(
            if (active) {
                Color.rgb(25, 145, 80)
            } else {
                Color.rgb(200, 50, 60)
            },
            if (active) {
                Color.rgb(20, 120, 65)
            } else {
                Color.rgb(170, 35, 45)
            },
            110,
            oval = true
        )

        sensitivitySlider.isEnabled = !active

        for (i in 0 until sensitivityLabels.childCount) {
            val label =
                sensitivityLabels.getChildAt(i) as TextView

            label.isEnabled = !active

            label.setTextColor(
                if (i == sensitivitySlider.progress) {
                    Color.rgb(25, 120, 65)
                } else {
                    Color.rgb(75, 80, 90)
                }
            )
        }
    }

    private fun setSensitivitySlider(index: Int) {
        if (!::sensitivitySlider.isInitialized) return

        updatingSlider = true
        sensitivitySlider.progress = index.coerceIn(0, 2)
        updatingSlider = false

        refreshSensitivityLabels(sensitivitySlider.progress)
    }

    private fun refreshSensitivityLabels(index: Int) {
        if (!::sensitivityLabels.isInitialized) return

        val unlocked = License.isUnlocked(this)

        for (i in 0 until sensitivityLabels.childCount) {
            val label =
                sensitivityLabels.getChildAt(i) as TextView

            val locked = i > 0 && !unlocked

            label.text = (if (locked) "🔒 " else "") +
                getString(choiceNames[i])

            label.setTextColor(
                if (i == index) {
                    Color.rgb(25, 120, 65)
                } else {
                    Color.rgb(75, 80, 90)
                }
            )
        }
    }

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
        handler.postDelayed(hideBanner, 3000)
    }

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

    private fun callOk(): Boolean {
        return checkSelfPermission(
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notifOk(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun batteryOk(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun allOk(): Boolean {
        return callOk() && notifOk() && batteryOk()
    }

    private fun refreshConfig() {
        if (!::configCard.isInitialized) return

        setBtn(callBtn, callOk())
        setBtn(notifBtn, notifOk())
        setBtn(batBtn, batteryOk())

        configCard.visibility =
            if (allOk()) View.GONE else View.VISIBLE
    }

    private fun askBattery() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
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
            setOnClickListener { onClick() }
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
            getString(R.string.btn_ok)
        } else {
            getString(R.string.btn_authorize)
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

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(6), 0, dp(8))
            }
        }
    }

    private fun createSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(90, 95, 105))
            setPadding(dp(4), dp(14), dp(4), dp(5))
        }
    }

    private fun roundedBackground(
        color: Int,
        strokeColor: Int,
        radius: Int,
        oval: Boolean = false
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = if (oval) {
                GradientDrawable.OVAL
            } else {
                GradientDrawable.RECTANGLE
            }

            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).roundToInt()
    }
}
