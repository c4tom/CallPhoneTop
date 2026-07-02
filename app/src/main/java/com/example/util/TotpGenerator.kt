package com.example.util

import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpGenerator {
    fun generateTOTP(
        secret: String,
        time: Long, // in seconds
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        try {
            val keyBytes = Base32.decode(secret)
            if (keyBytes.isEmpty()) return "000000"

            val counter = time / period
            val msg = ByteArray(8)
            var v = counter
            for (i in 7 downTo 0) {
                msg[i] = (v and 0xFF).toByte()
                v = v ushr 8
            }

            val hmacAlgo = when (algorithm.uppercase()) {
                "SHA256", "HMACSHA256" -> "HmacSHA256"
                "SHA512", "HMACSHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }

            val mac = Mac.getInstance(hmacAlgo)
            mac.init(SecretKeySpec(keyBytes, hmacAlgo))
            val hash = mac.doFinal(msg)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % Math.pow(10.0, digits.toDouble()).toLong()
            return String.format("%0${digits}d", otp)
        } catch (e: Exception) {
            return "000000"
        }
    }
}
