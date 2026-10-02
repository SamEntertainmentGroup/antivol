package com.samir.antivol

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.net.Uri
import android.os.*
import android.telecom.TelecomManager
import kotlin.math.abs
import kotlin.math.sqrt

class MotionService : Service(), SensorEventListener {

    private val ARMEMENT = 30_000L
    private val PAUSE = 60_000L
    private var seuil = 2.0f
    private var armedAt = 0L
    private var lastCall = 0L
    private lateinit var sm: SensorManager
    private lateinit var wl: PowerManager.WakeLock

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("av", "Antivol", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "av")
            .setContentTitle("Antivol actif")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build())

        wl = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "antivol:wl")
        if (!wl.isHeld) wl.acquire()

        seuil = getSharedPreferences("p", MODE_PRIVATE).getFloat("seuil", 2.0f)
        armedAt = System.currentTimeMillis() + ARMEMENT

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.unregisterListener(this)
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI)
        return START_STICKY
    }

    override fun onSensorChanged(e: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now < armedAt || now - lastCall < PAUSE) return
        val v = e.values
        val mag = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        if (abs(mag - 9.81f) > seuil) {
            lastCall = now
            appeler()
        }
    }

    @Suppress("DEPRECATION")
    private fun allumerEcran() {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "antivol:ecran")
            .acquire(5000)
    }

    private fun appeler() {
        val p = getSharedPreferences("p", MODE_PRIVATE)
        val num = p.getString("num", "") ?: ""
        if (num.isEmpty()) return
        if (p.getBoolean("ecran", false)) allumerEcran()
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                (getSystemService(Context.TELECOM_SERVICE) as TelecomManager)
                    .placeCall(Uri.fromParts("tel", num, null), Bundle())
            } catch (_: Exception) {}
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        sm.unregisterListener(this)
        if (wl.isHeld) wl.release()
        super.onDestroy()
    }
}
