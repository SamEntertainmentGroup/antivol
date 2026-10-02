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

        // Armement après 30 secondes
        private const val ARMEMENT =
            30_000L

        // Pause entre deux appels
        private const val PAUSE =
            60_000L
    }

    private var seuil = 0.8f

    private var armedAt = 0L

    private var lastCall = 0L

    private lateinit var sensorManager:
            SensorManager

    private var wakeLock:
            PowerManager.WakeLock? = null

    // =============================================================
    // CRÉATION
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

        armedAt =
            System.currentTimeMillis() +
                    ARMEMENT

        val accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        if (accelerometer == null) {

            stopSelf()

            return START_NOT_STICKY
        }

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
    // DÉTECTION
    // =============================================================

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        val now =
            System.currentTimeMillis()

        // Attente des 30 secondes d'armement
        if (now < armedAt) {
            return
        }

        // Cooldown de 30 secondes
        if (now - lastCall < PAUSE) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

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

        /*
         * Détection sensible.
         *
         * Un mouvement même léger peut
         * déclencher l'appel.
         */
        if (movement > seuil) {

            lastCall = now

            appeler()
        }
    }

    // =============================================================
    // APPEL
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

        // Allume l'écran avant de lancer l'appel
        val powerManager =
            getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        val screenWakeLock =
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "antivol:call_screen"
            )

        screenWakeLock.acquire(5000L)

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
                // Appel impossible
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
                "Protection antivol active"
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
    // CALLBACKS
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
    // ARRÊT
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
