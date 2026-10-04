package com.samir.antivol

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.*
import kotlin.math.roundToInt

class PlusActivity : Activity() {

    // Remplace par ton numéro WhatsApp : indicatif sans + ni 0 devant (ex : 213680912224)
    private val SUPPORT_WHATSAPP = "213555123456"

    private val NOIR = Color.rgb(35, 38, 45)

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

        // COMMENT ÇA MARCHE
        root.addView(sectionTitle("Comment ça marche"))
        val howCard = card()
        howCard.addView(TextView(this).apply {
            text = Html.fromHtml(
                "<b>1. Prépare le téléphone.</b> Laisse dans la voiture un téléphone avec une puce ayant au moins une unité de crédit, pour qu'il puisse appeler. Pas besoin d'internet." +
                    "<br><br>" +
                    "<b>2. Active la protection.</b> Entre ton numéro personnel (ou ton deuxième mobile), choisis la sensibilité, puis appuie sur « Activer la protection »." +
                    "<br><br>" +
                    "<b>3. Reçois l'alerte.</b> Si la voiture bouge, le téléphone t'appelle. Ne décroche pas : rejette l'appel et va voir ta voiture.",
                Html.FROM_HTML_MODE_LEGACY
            )
            textSize = 15f
            setTextColor(NOIR)
            setLineSpacing(0f, 1.15f)
        })
        root.addView(howCard)

        // CONTACTER LE SUPPORT
        root.addView(
            grayButton("Contacter le support") { openSupport() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { topMargin = dp(16) }
        )

        // PARTAGER
        root.addView(
            grayButton("🔗 Partager l'application") { share() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { topMargin = dp(12) }
        )

        // VÉRIFIER LES MISES À JOUR
        root.addView(
            grayButton("🔄 Vérifier les mises à jour") { checkUpdate() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { topMargin = dp(12) }
        )

        // RETOUR
        root.addView(
            backButton("Retour à l'accueil") { finish() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { topMargin = dp(28) }
        )

        setContentView(scroll)
    }

    // ACTIONS
    private fun openSupport() {
        val msg = "Bonjour, j'ai besoin d'aide avec l'application Antivol.\n\n" +
            "Téléphone : ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Android : ${Build.VERSION.RELEASE}\n" +
            "Version de l'application : ${versionName()}\n\n" +
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
                "Antivol : une application qui appelle ton téléphone si ta voiture bouge pendant la nuit.\n\n" +
                    "https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        startActivity(Intent.createChooser(intent, "Partager l'application"))
    }

    private fun checkUpdate() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun versionName(): String =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

    // OUTILS D'INTERFACE
    private fun grayButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(NOIR)
            background = roundedBackground(
                Color.rgb(235, 236, 239),
                Color.rgb(220, 223, 229),
                12
            )
            stateListAnimator = null
            setOnClickListener { onClick() }
        }
    }

    private fun backButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(NOIR)
            background = roundedBackground(Color.WHITE, NOIR, 12)
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
