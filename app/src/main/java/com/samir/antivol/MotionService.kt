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

    private val seuil = 2.0f
    private val armement = 30_000L
    private val pause = 60_000L

    private var armedAt = 0L
    private var lastCall = 0L

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            "antivol",
            "Antivol",
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, "antivol")
            .setContentTitle("Antivol actif")
            .setContentText("Détection de mouvement activée")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()

        startForeground(1, notification)

        val powerManager =
            getSystemService(Context.POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "antivol:wakelock"
        )

        if (!wakeLock.isHeld) {
            wakeLock.acquire()
        }

        armedAt = System.currentTimeMillis() + armement

        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.unregisterListener(this)

        val accelerometer =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {

        val now = System.currentTimeMillis()

        if (now < armedAt) return
        if (now - lastCall < pause) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(
            x * x + y * y + z * z
        )

        if (abs(magnitude - 9.81f) > seuil) {
            lastCall = now
            appeler()
        }
    }

    private fun appeler() {

        val numero = getSharedPreferences(
            "p",
            MODE_PRIVATE
        ).getString("num", "") ?: return

        if (numero.isEmpty()) return

        try {
            val telecomManager =
                getSystemService(Context.TELECOM_SERVICE) as TelecomManager

            telecomManager.placeCall(
                Uri.fromParts("tel", numero, null),
                Bundle()
            )

        } catch (_: SecurityException) {
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {

        sensorManager.unregisterListener(this)

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        super.onDestroy()
    }
}
