package com.example.util

import android.util.Base64
import com.example.data.OtpEntry
import com.example.data.VaultEntry
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecureBackupManager {
    private const val ITERATIONS = 15000
    private const val KEY_LENGTH = 256
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val MARKER = "SECURE_AUTHENTICATOR_VAULT_BACKUP_v1"

    /**
     * Generates a 256-bit AES key derived from the master password using PBKDF2.
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the plaintext backup string using the master password.
     * Returns a JSON string containing the version, salt, IV, and ciphertext.
     */
    fun encryptBackup(plaintext: String, masterPassword: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iv = ByteArray(12) // Standard GCM IV length
        random.nextBytes(iv)

        val keySpec = deriveKey(masterPassword, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val backupObj = JSONObject().apply {
            put("version", 1)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP))
        }

        return backupObj.toString(2)
    }

    /**
     * Decrypts an encrypted backup string using the master password.
     * Throws an exception or returns null if decryption or integrity validation fails.
     */
    fun decryptBackup(backupJsonStr: String, masterPassword: String): String? {
        return try {
            val backupObj = JSONObject(backupJsonStr)
            val version = backupObj.optInt("version", 1)
            val saltBase64 = backupObj.getString("salt")
            val ivBase64 = backupObj.getString("iv")
            val ciphertextBase64 = backupObj.getString("ciphertext")

            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

            val keySpec = deriveKey(masterPassword, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Serializes OTP and Vault entries into a unified plaintext JSON structure.
     */
    fun createPlaintextBackup(
        otpEntries: List<OtpEntry>,
        vaultEntries: List<VaultEntry>,
        otpDecryptor: (encryptedSecret: String, iv: String) -> String,
        vaultDecryptor: (encrypted: String, iv: String) -> String
    ): String {
        val root = JSONObject().apply {
            put("marker", MARKER)
            put("timestamp", System.currentTimeMillis())

            // Serialize OTP entries
            val otpArray = JSONArray()
            for (entry in otpEntries) {
                val secretDecrypted = otpDecryptor(entry.encryptedSecret, entry.iv)
                val entryObj = JSONObject().apply {
                    put("displayName", entry.displayName)
                    put("issuer", entry.issuer)
                    put("label", entry.label)
                    put("secret", secretDecrypted)
                    put("algorithm", entry.algorithm)
                    put("digits", entry.digits)
                    put("period", entry.period)
                    put("displayOrder", entry.displayOrder)
                }
                otpArray.put(entryObj)
            }
            put("otp_entries", otpArray)

            // Serialize Vault entries
            val vaultArray = JSONArray()
            for (entry in vaultEntries) {
                val userDecrypted = vaultDecryptor(entry.usernameEncrypted, entry.usernameIv)
                val passDecrypted = vaultDecryptor(entry.passwordEncrypted, entry.passwordIv)
                val notesDecrypted = vaultDecryptor(entry.notesEncrypted, entry.notesIv)

                val entryObj = JSONObject().apply {
                    put("title", entry.title)
                    put("username", userDecrypted)
                    put("password", passDecrypted)
                    put("url", entry.url)
                    put("notes", notesDecrypted)
                    put("category", entry.category)
                    put("isFavorite", entry.isFavorite)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                }
                vaultArray.put(entryObj)
            }
            put("vault_entries", vaultArray)
        }
        return root.toString()
    }

    /**
     * Validates and parses the decrypted backup, yielding lists of entries to import.
     */
    fun parseDecryptedBackup(plaintext: String): BackupContent? {
        return try {
            val root = JSONObject(plaintext)
            val marker = root.optString("marker", "")
            if (marker != MARKER) return null

            val otpList = mutableListOf<ParsedOtpEntry>()
            val otpArray = root.optJSONArray("otp_entries")
            if (otpArray != null) {
                for (i in 0 until otpArray.length()) {
                    val obj = otpArray.getJSONObject(i)
                    otpList.add(
                        ParsedOtpEntry(
                            displayName = obj.getString("displayName"),
                            issuer = obj.getString("issuer"),
                            label = obj.getString("label"),
                            secret = obj.getString("secret"),
                            algorithm = obj.optString("algorithm", "SHA1"),
                            digits = obj.optInt("digits", 6),
                            period = obj.optInt("period", 30),
                            displayOrder = obj.optInt("displayOrder", 0)
                        )
                    )
                }
            }

            val vaultList = mutableListOf<ParsedVaultEntry>()
            val vaultArray = root.optJSONArray("vault_entries")
            if (vaultArray != null) {
                for (i in 0 until vaultArray.length()) {
                    val obj = vaultArray.getJSONObject(i)
                    vaultList.add(
                        ParsedVaultEntry(
                            title = obj.getString("title"),
                            username = obj.optString("username", ""),
                            password = obj.optString("password", ""),
                            url = obj.optString("url", ""),
                            notes = obj.optString("notes", ""),
                            category = obj.optString("category", "Login"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            BackupContent(otpList, vaultList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class ParsedOtpEntry(
        val displayName: String,
        val issuer: String,
        val label: String,
        val secret: String,
        val algorithm: String,
        val digits: Int,
        val period: Int,
        val displayOrder: Int
    )

    data class ParsedVaultEntry(
        val title: String,
        val username: String,
        val password: String,
        val url: String,
        val notes: String,
        val category: String,
        val isFavorite: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    data class BackupContent(
        val otpEntries: List<ParsedOtpEntry>,
        val vaultEntries: List<ParsedVaultEntry>
    )
}
