package com.samir.antivol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.telecom.TelecomManager
import kotlin.math.abs
import kotlin.math.sqrt

class MotionService : Service(), SensorEventListener {

    companion object {

        private const val CHANNEL_ID =
            "antivol_channel"

        private const val NOTIFICATION_ID = 1

        // Temps avant armement
        private const val ARMEMENT =
            30_000L

        // Pause entre deux appels
        private const val PAUSE =
            60_000L
    }

    private var seuil = 0.8f

    private var armedAt = 0L

    private var lastCall = 0L

    private lateinit var sensorManager: SensorManager

    private var wakeLock:
            PowerManager.WakeLock? = null

    // =============================================================
    // CRÉATION DU SERVICE
    // =============================================================

    override fun onCreate() {

        super.onCreate()

        sensorManager =
            getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager

        createNotificationChannel()

        val powerManager =
            getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "antivol:motion"
            )

        wakeLock?.acquire()
    }

    // =============================================================
    // DÉMARRAGE
    // =============================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        val prefs =
            getSharedPreferences(
                "p",
                MODE_PRIVATE
            )

        seuil =
            prefs.getFloat(
                "seuil",
                0.8f
            )

        // Armement dans 30 secondes
        armedAt =
            System.currentTimeMillis() +
                    ARMEMENT

        val accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        // Pas d'accéléromètre
        if (accelerometer == null) {

            stopSelf()

            return START_NOT_STICKY
        }

        // Évite plusieurs écouteurs
        sensorManager.unregisterListener(
            this
        )

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        return START_STICKY
    }

    // =============================================================
    // DÉTECTION DU MOUVEMENT
    // =============================================================

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        val now =
            System.currentTimeMillis()

        // Les 30 secondes d'armement
        if (now < armedAt) {
            return
        }

        // Pause après un appel
        if (now - lastCall < PAUSE) {
            return
        }

        val x =
            event.values[0]

        val y =
            event.values[1]

        val z =
            event.values[2]

        /*
         * Calcul de l'accélération totale.
         *
         * Lorsque le téléphone est immobile,
         * la valeur est proche de 9.81 m/s²
         * à cause de la gravité.
         */
        val magnitude =
            sqrt(
                x * x +
                        y * y +
                        z * z
            )

        val movement =
            abs(
                magnitude - 9.81f
            )

        // Mouvement détecté
        if (movement > seuil) {

            lastCall = now

            appeler()
        }
    }

    // =============================================================
    // APPEL DU NUMÉRO D'ALERTE
    // =============================================================

    private fun appeler() {

        val prefs =
            getSharedPreferences(
                "p",
                MODE_PRIVATE
            )

        val num =
            prefs.getString(
                "num",
                ""
            ) ?: ""

        if (num.isEmpty()) {
            return
        }

        val uri =
            Uri.parse(
                "tel:$num"
            )

        try {

            val intent =
                Intent(
                    Intent.ACTION_CALL,
                    uri
                ).apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(intent)

        } catch (_: Exception) {

            /*
             * Deuxième méthode de secours
             * si ACTION_CALL échoue.
             */

            try {

                val telecom =
                    getSystemService(
                        Context.TELECOM_SERVICE
                    ) as TelecomManager

                telecom.placeCall(
                    Uri.fromParts(
                        "tel",
                        num,
                        null
                    ),
                    Bundle()
                )

            } catch (_: Exception) {
                // Impossible de lancer l'appel
            }
        }
    }

    // =============================================================
    // NOTIFICATION
    // =============================================================

    private fun createNotificationChannel() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Protection antivol",
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                description =
                    "Surveillance antivol active"

                setShowBadge(false)
            }

        manager.createNotificationChannel(
            channel
        )
    }

    private fun createNotification():
            Notification {

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_lock
            )
            .setContentTitle(
                "Antivol actif"
            )
            .setContentText(
                "Surveillance des mouvements en cours"
            )
            .setOngoing(true)
            .setCategory(
                Notification.CATEGORY_SERVICE
            )
            .build()
    }

    // =============================================================
    // AUTRES CALLBACKS
    // =============================================================

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    // =============================================================
    // DESTRUCTION
    // =============================================================

    override fun onDestroy() {

        sensorManager.unregisterListener(
            this
        )

        wakeLock?.let {

            if (it.isHeld) {
                it.release()
            }
        }

        wakeLock = null

        super.onDestroy()
    }
}
