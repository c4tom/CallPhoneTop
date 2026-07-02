package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.OtpEntry
import com.example.data.OtpRepository
import com.example.util.Base32
import com.example.util.CryptoManager
import com.example.util.QrCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class OtpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: OtpRepository
    
    val allEntries: StateFlow<List<OtpEntry>>
    val searchQuery = MutableStateFlow("")
    val filteredEntries: StateFlow<List<OtpEntry>>

    // Flow of current time updated every second
    val currentTime = flow {
        while (true) {
            emit(System.currentTimeMillis() / 1000)
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis() / 1000)

    // Tracks remaining seconds for revealed secrets (key: entryId, value: seconds remaining)
    private val _revealedEntries = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val revealedEntries: StateFlow<Map<Long, Int>> = _revealedEntries.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OtpRepository(database.otpEntryDao())
        allEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        filteredEntries = combine(allEntries, searchQuery) { entries, query ->
            if (query.isBlank()) {
                entries
            } else {
                entries.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                    it.issuer.contains(query, ignoreCase = true) ||
                    it.label.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Start a timer to handle auto-masking countdowns
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _revealedEntries.value
                if (current.isNotEmpty()) {
                    val updated = current.mapValues { it.value - 1 }
                        .filter { it.value > 0 }
                    _revealedEntries.value = updated
                }
            }
        }
    }

    fun revealSecret(entryId: Long) {
        val current = _revealedEntries.value.toMutableMap()
        current[entryId] = 10
        _revealedEntries.value = current
    }

    fun hideSecret(entryId: Long) {
        val current = _revealedEntries.value.toMutableMap()
        current.remove(entryId)
        _revealedEntries.value = current
    }

    fun hideAllSecrets() {
        _revealedEntries.value = emptyMap()
    }

    fun addManual(
        displayName: String,
        issuer: String,
        label: String,
        secret: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30
    ): Boolean {
        val normalizedSecret = secret.uppercase().replace("[^A-Z2-7]".toRegex(), "")
        if (normalizedSecret.isEmpty()) return false

        viewModelScope.launch {
            // Check if secret already exists to deduplicate
            val exists = allEntries.value.any {
                val decrypted = CryptoManager.decrypt(it.encryptedSecret, it.iv)
                decrypted == normalizedSecret
            }
            if (exists) return@launch

            val (encrypted, iv) = CryptoManager.encrypt(normalizedSecret)
            val entry = OtpEntry(
                displayName = displayName.ifBlank { if (issuer.isNotBlank()) "$issuer ($label)" else label },
                issuer = issuer,
                label = label,
                encryptedSecret = encrypted,
                iv = iv,
                algorithm = algorithm,
                digits = digits,
                period = period
            )
            repository.insertEntry(entry)
        }
        return true
    }

    fun deleteEntry(entry: OtpEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun updateDisplayName(entry: OtpEntry, newName: String) {
        viewModelScope.launch {
            repository.updateEntry(entry.copy(displayName = newName))
        }
    }

    fun moveEntryUp(entry: OtpEntry) {
        val list = allEntries.value
        val index = list.indexOfFirst { it.id == entry.id }
        if (index > 0) {
            val target = list[index - 1]
            viewModelScope.launch {
                val tempOrder = entry.displayOrder
                repository.updateEntry(entry.copy(displayOrder = target.displayOrder))
                repository.updateEntry(target.copy(displayOrder = tempOrder))
            }
        }
    }

    fun moveEntryDown(entry: OtpEntry) {
        val list = allEntries.value
        val index = list.indexOfFirst { it.id == entry.id }
        if (index != -1 && index < list.size - 1) {
            val target = list[index + 1]
            viewModelScope.launch {
                val tempOrder = entry.displayOrder
                repository.updateEntry(entry.copy(displayOrder = target.displayOrder))
                repository.updateEntry(target.copy(displayOrder = tempOrder))
            }
        }
    }

    fun decryptSecret(entry: OtpEntry): String {
        return CryptoManager.decrypt(entry.encryptedSecret, entry.iv)
    }

    // Directly add from raw text scanned by camera
    fun addFromRawQrText(qrText: String, onResult: (ImportStatus) -> Unit) {
        if (qrText.startsWith("otpauth-migration://")) {
            onResult(ImportStatus.ErrorBulkMigrationNotSupported)
            return
        }

        val details = parseOtpUri(qrText)
        if (details == null) {
            onResult(ImportStatus.ErrorInvalidUri)
            return
        }

        viewModelScope.launch {
            val normalizedSecret = details.secret.uppercase().replace("[^A-Z2-7]".toRegex(), "")
            val exists = allEntries.value.any {
                val decrypted = CryptoManager.decrypt(it.encryptedSecret, it.iv)
                decrypted == normalizedSecret
            }
            if (exists) {
                onResult(ImportStatus.SuccessDuplicateSkipped)
                return@launch
            }

            val (encrypted, iv) = CryptoManager.encrypt(normalizedSecret)
            val entry = OtpEntry(
                displayName = details.displayName,
                issuer = details.issuer,
                label = details.label,
                encryptedSecret = encrypted,
                iv = iv,
                algorithm = details.algorithm,
                digits = details.digits,
                period = details.period
            )
            repository.insertEntry(entry)
            onResult(ImportStatus.SuccessAdded(details.displayName))
        }
    }

    // QR Image decoding and handling
    fun decodeAndAddFromQr(bitmap: Bitmap, onResult: (ImportStatus) -> Unit) {
        val qrText = QrCodeHelper.decodeQrCode(bitmap)
        if (qrText == null) {
            onResult(ImportStatus.ErrorInvalidQr)
            return
        }

        if (qrText.startsWith("otpauth-migration://")) {
            onResult(ImportStatus.ErrorBulkMigrationNotSupported)
            return
        }

        val details = parseOtpUri(qrText)
        if (details == null) {
            onResult(ImportStatus.ErrorInvalidUri)
            return
        }

        // Add
        viewModelScope.launch {
            val normalizedSecret = details.secret.uppercase().replace("[^A-Z2-7]".toRegex(), "")
            val exists = allEntries.value.any {
                val decrypted = CryptoManager.decrypt(it.encryptedSecret, it.iv)
                decrypted == normalizedSecret
            }
            if (exists) {
                onResult(ImportStatus.SuccessDuplicateSkipped)
                return@launch
            }

            val (encrypted, iv) = CryptoManager.encrypt(normalizedSecret)
            val entry = OtpEntry(
                displayName = details.displayName,
                issuer = details.issuer,
                label = details.label,
                encryptedSecret = encrypted,
                iv = iv,
                algorithm = details.algorithm,
                digits = details.digits,
                period = details.period
            )
            repository.insertEntry(entry)
            onResult(ImportStatus.SuccessAdded(details.displayName))
        }
    }

    // Export list of OTPs to a JSON backup
    fun exportBackupJson(): String {
        val array = JSONArray()
        for (entry in allEntries.value) {
            val secret = decryptSecret(entry)
            val obj = JSONObject().apply {
                put("displayName", entry.displayName)
                put("issuer", entry.issuer)
                put("label", entry.label)
                put("secret", secret)
                put("algorithm", entry.algorithm)
                put("digits", entry.digits)
                put("period", entry.period)
                put("displayOrder", entry.displayOrder)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    // Import OTP list from a JSON backup (duplicates skipped)
    fun importBackupJson(jsonString: String, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                var importedCount = 0
                var skippedCount = 0

                val currentSecrets = allEntries.value.map { decryptSecret(it) }.toSet()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val secret = obj.optString("secret", "").uppercase().replace("[^A-Z2-7]".toRegex(), "")
                    if (secret.isEmpty()) {
                        skippedCount++
                        continue
                    }

                    if (currentSecrets.contains(secret)) {
                        skippedCount++
                        continue
                    }

                    val displayName = obj.optString("displayName", "")
                    val issuer = obj.optString("issuer", "")
                    val label = obj.optString("label", "")
                    val algorithm = obj.optString("algorithm", "SHA1")
                    val digits = obj.optInt("digits", 6)
                    val period = obj.optInt("period", 30)

                    val (encrypted, iv) = CryptoManager.encrypt(secret)
                    val entry = OtpEntry(
                        displayName = displayName.ifBlank { if (issuer.isNotBlank()) "$issuer ($label)" else label },
                        issuer = issuer,
                        label = label,
                        encryptedSecret = encrypted,
                        iv = iv,
                        algorithm = algorithm,
                        digits = digits,
                        period = period
                    )
                    repository.insertEntry(entry)
                    importedCount++
                }
                onResult(importedCount, skippedCount)
            } catch (e: Exception) {
                onResult(-1, 0)
            }
        }
    }

    private fun parseOtpUri(uriString: String): OtpDetails? {
        val uri = android.net.Uri.parse(uriString) ?: return null
        if (uri.scheme != "otpauth" || uri.host != "totp") return null

        val secret = uri.getQueryParameter("secret") ?: return null
        var issuer = uri.getQueryParameter("issuer") ?: ""
        var label = uri.path?.trimStart('/') ?: ""

        if (label.contains(":")) {
            val parts = label.split(":", limit = 2)
            val partIssuer = parts[0].trim()
            val partLabel = parts[1].trim()
            if (issuer.isEmpty()) {
                issuer = partIssuer
            }
            label = partLabel
        }

        val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"
        val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

        return OtpDetails(
            displayName = if (issuer.isNotEmpty()) "$issuer ($label)" else label,
            issuer = issuer,
            label = label,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period
        )
    }
}

sealed interface ImportStatus {
    object ErrorInvalidQr : ImportStatus
    object ErrorBulkMigrationNotSupported : ImportStatus
    object ErrorInvalidUri : ImportStatus
    object SuccessDuplicateSkipped : ImportStatus
    data class SuccessAdded(val name: String) : ImportStatus
}

data class OtpDetails(
    val displayName: String,
    val issuer: String,
    val label: String,
    val secret: String,
    val algorithm: String,
    val digits: Int,
    val period: Int
)
