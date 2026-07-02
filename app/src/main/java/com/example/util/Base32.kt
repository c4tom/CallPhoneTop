package com.example.util

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val CHAR_TO_VAL = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun decode(input: String): ByteArray {
        val cleaned = input.uppercase().replace("[^A-Z2-7]".toRegex(), "")
        if (cleaned.isEmpty()) return ByteArray(0)
        
        val len = cleaned.length
        val outLen = len * 5 / 8
        val out = ByteArray(outLen)
        
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        
        for (i in 0 until len) {
            val charCode = cleaned[i].code
            if (charCode >= 128) continue
            val value = CHAR_TO_VAL[charCode]
            if (value < 0) continue
            
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out[count++] = (buffer shr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return out.copyOf(count)
    }

    fun encode(data: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                out.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            out.append(ALPHABET[index])
        }
        return out.toString()
    }
}
