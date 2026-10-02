package com.samir.antivol

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlin.math.roundToInt

class PlusActivity : Activity() {

    // Remplace par ton numéro WhatsApp : indicatif sans + ni 0 devant (ex : 213555123456)
    private val SUPPORT_WHATSAPP = "213555123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(20), dp(28), dp(20), dp(20))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        // EN-TÊTE
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "←"
            textSize = 28f
            setTextColor(Color.rgb(25, 28, 35))
            setPadding(dp(4), dp(4), dp(18), dp(4))
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Plus"
            textSize = 29f
            setTextColor(Color.rgb(25, 28, 35))
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(header)

        // COMMENT ÇA MARCHE
        root.addView(sectionTitle("Comment ça marche"))
        val howCard = card()
        howCard.addView(TextView(this).apply {
            text = "1. Entre le numéro à appeler en cas d'alerte.\n\n" +
                "2. Choisis la sensibilité.\n\n" +
                "3. Appuie sur « Activer la protection ».\n\n" +
                "4. Laisse le téléphone dans la voiture : s'il bouge ou vibre, il appelle ton numéro.\n\n" +
                "5. Rejette l'appel et va voir ta voiture."
            textSize = 15f
            setTextColor(Color.rgb(35, 38, 45))
        })
        root.addView(howCard)

        // SUPPORT
        root.addView(sectionTitle("Aide"))
        val helpCard = card()
        helpCard.addView(TextView(this).apply {
            text = "Un problème ou une question ? Écris-nous sur WhatsApp. " +
                "Le message contient déjà les infos de ton téléphone."
            textSize = 15f
            setTextColor(Color.rgb(35, 38, 45))
        })
        helpCard.addView(
            actionButton("Contacter le support", Color.rgb(37, 160, 90), Color.WHITE, Color.TRANSPARENT) {
                openSupport()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(14) }
        )
        root.addView(helpCard)

        // PARTAGER
        root.addView(sectionTitle("Partager"))
        val shareCard = card()
        shareCard.addView(TextView(this).apply {
            text = "Fais découvrir l'appli à tes amis et à ta famille."
            textSize = 15f
            setTextColor(Color.rgb(35, 38, 45))
        })
        shareCard.addView(
            actionButton("Partager l'appli", Color.WHITE, Color.rgb(25, 118, 210), Color.rgb(25, 118, 210)) {
                share()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(14) }
        )
        root.addView(shareCard)

        // PREMIUM (à venir)
        root.addView(sectionTitle("Premium"))
        val premiumCard = card()
        premiumCard.addView(TextView(this).apply {
            text = "⭐ Bientôt disponible"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(35, 38, 45))
        })
        premiumCard.addView(TextView(this).apply {
            text = "Réglages avancés et plus d'options."
            textSize = 14f
            setTextColor(Color.rgb(90, 95, 105))
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(premiumCard)

        // VERSION
        root.addView(TextView(this).apply {
            text = "Antivol · version ${versionName()}"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(140, 145, 155))
            setPadding(0, dp(22), 0, dp(10))
        })

        setContentView(scroll)
    }

    // ACTIONS
    private fun openSupport() {
        val msg = "Bonjour, j'ai besoin d'aide avec l'appli Antivol.\n\n" +
            "Téléphone : ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Android : ${Build.VERSION.RELEASE}\n" +
            "Version de l'appli : ${versionName()}\n\n" +
            "Mon problème : "
        val uri = Uri.parse("https://wa.me/$SUPPORT_WHATSAPP?text=${Uri.encode(msg)}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Antivol : une appli qui appelle ton téléphone si ta voiture bouge pendant la nuit."
            )
        }
        startActivity(Intent.createChooser(intent, "Partager l'appli"))
    }

    private fun versionName(): String =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

    // OUTILS D'INTERFACE
    private fun actionButton(
        label: String,
        bg: Int,
        fg: Int,
        stroke: Int,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(fg)
            background = roundedBackground(bg, stroke, 12)
            stateListAnimator = null
            setOnClickListener { onClick() }
        }
    }

    private fun card(): LinearLayout {
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

    private fun sectionTitle(text: String): TextView {
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}
