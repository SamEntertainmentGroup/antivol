
package com.autoalert.app

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object License {

    // À remplacer par une clé secrète personnelle.
    // La même clé devra être utilisée dans codes.php.
    private const val SECRET = "CHANGE-THIS-LONG-RANDOM-SECRET"

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val EPOCH_DAYS = 20089L
    private const val MAC_BITS = 34
    private const val DAY_MS = 86_400_000L

    private fun today(): Long =
        System.currentTimeMillis() / DAY_MS - EPOCH_DAYS

    fun deviceCode(ctx: Context): String {
        val id = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val hash = MessageDigest.getInstance("SHA-256")
            .digest("antivol:$id".toByteArray())

        var value = 0L
        for (i in 0 until 5) {
            value = (value shl 8) or (hash[i].toLong() and 0xFF)
        }

        return encode(value, 8)
    }

    fun deviceCodeDisplay(ctx: Context): String {
        val code = deviceCode(ctx)
        return code.substring(0, 4) + "-" + code.substring(4)
    }

    // 0 = valide, 1 = invalide, 2 = expiré
    fun activate(ctx: Context, raw: String): Int {
        val code = normalize(raw)
        val expiry = expiryOf(ctx, code) ?: return 1

        if (expiry < today()) return 2

        ctx.getSharedPreferences("p", Context.MODE_PRIVATE)
            .edit()
            .putString("unlock_code", code)
            .apply()

        return 0
    }

    fun unlockedUntil(ctx: Context): Long? {
        val code = ctx.getSharedPreferences("p", Context.MODE_PRIVATE)
            .getString("unlock_code", null) ?: return null

        val expiry = expiryOf(ctx, code) ?: return null
        return if (expiry >= today()) expiry else null
    }

    fun isUnlocked(ctx: Context): Boolean =
        unlockedUntil(ctx) != null

    fun formatDate(day: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date((day + EPOCH_DAYS) * DAY_MS))

    private fun normalize(raw: String): String =
        raw.uppercase().filter { ALPHABET.indexOf(it) >= 0 }

    private fun expiryOf(ctx: Context, code: String): Long? {
        if (code.length != 10) return null

        val value = decode(code) ?: return null
        val expiry = value ushr MAC_BITS
        val macValue = value and ((1L shl MAC_BITS) - 1)

        return if (mac(deviceCode(ctx), expiry) == macValue) {
            expiry
        } else {
            null
        }
    }

    private fun mac(device: String, expiry: Long): Long {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))

        val hash = mac.doFinal("$device:$expiry".toByteArray())

        var value = 0L
        for (i in 0 until 5) {
            value = (value shl 8) or (hash[i].toLong() and 0xFF)
        }

        return value and ((1L shl MAC_BITS) - 1)
    }

    private fun encode(value: Long, length: Int): String {
        val result = StringBuilder()

        for (i in length - 1 downTo 0) {
            result.append(
                ALPHABET[((value shr (5 * i)) and 31L).toInt()]
            )
        }

        return result.toString()
    }

    private fun decode(code: String): Long? {
        var value = 0L

        for (char in code) {
            val index = ALPHABET.indexOf(char)
            if (index < 0) return null

            value = (value shl 5) or index.toLong()
        }

        return value
    }
}
