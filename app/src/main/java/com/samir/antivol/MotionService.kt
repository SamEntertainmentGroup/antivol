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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telecom.TelecomManager
import kotlin.math.abs
import kotlin.math.max

class MotionService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL_ID = "antivol_channel"
        private const val NOTIFICATION_ID = 1

        // Armement après 30 secondes
        private const val ARMEMENT = 30_000L

        // Durée de chaque appel avant raccrochage automatique
        private const val SONNERIE = 20_000L

        // Pause entre la fin d'un appel et le suivant
        private const val ATTENTE = 10_000L
    }

    private var seuil = 0.3f
    private var armedAt = 0L
    private var nextCallAt = 0L

    private var hasLast = false
    private var lx = 0f
    private var ly = 0f
    private var lz = 0f

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sensorManager: SensorManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "antivol:motion")
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        seuil = getSharedPreferences("p", MODE_PRIVATE).getFloat("seuil", 0.3f)

        val now = System.currentTimeMillis()
        armedAt = now + ARMEMENT
        nextCallAt = 0L
        hasLast = false

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        return START_STICKY
    }

    // DÉTECTION : variation entre deux mesures, quelle que soit la position
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!hasLast) {
            lx = x; ly = y; lz = z
            hasLast = true
            return
        }

        val delta = max(abs(x - lx), max(abs(y - ly), abs(z - lz)))
        lx = x; ly = y; lz = z

        val now = System.currentTimeMillis()
        if (now < armedAt || now < nextCallAt) return

        if (delta > seuil) {
            nextCallAt = now + SONNERIE + ATTENTE
            appeler()
        }
    }

    // APPEL
    private fun appeler() {
        val num = getSharedPreferences("p", MODE_PRIVATE).getString("num", "") ?: ""
        if (num.isEmpty()) return

        try {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.placeCall(Uri.fromParts("tel", num, null), Bundle())
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }

        handler.postDelayed({ raccrocher() }, SONNERIE)
    }

    @Suppress("DEPRECATION")
    private fun raccrocher() {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                (getSystemService(Context.TELECOM_SERVICE) as TelecomManager).endCall()
            } catch (_: Exception) {
            }
        }
    }

    // NOTIFICATION
    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Protection antivol",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Surveillance antivol active"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Protection antivol active")
            .setContentText("Surveillance des mouvements en cours")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }
}
