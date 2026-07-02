package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VaultEntry
import com.example.data.VaultRepository
import com.example.data.OtpEntry
import com.example.data.OtpRepository
import com.example.util.CryptoManager
import com.example.util.SecureBackupManager
import com.example.util.CloudStorageManager
import com.example.util.CloudConfig
import com.example.util.CloudProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VaultRepository
    private val otpRepository: OtpRepository
    private val sharedPrefs = application.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    // Collect all OTP entries for backup
    private val allOtpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())

    // Master Password States
    private val _isMasterPasswordSet = MutableStateFlow(false)
    val isMasterPasswordSet: StateFlow<Boolean> = _isMasterPasswordSet.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Inactivity Auto-Lock States
    val secondsRemaining = MutableStateFlow(300)
    private var lastInteractionTime = System.currentTimeMillis()

    fun resetInactivityTimer() {
        lastInteractionTime = System.currentTimeMillis()
        secondsRemaining.value = 300
    }

    // Vault Entries States
    val allEntries = MutableStateFlow<List<VaultEntry>>(emptyMap<Long, VaultEntry>().values.toList())
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Todos") // Todos, Login, Cartão, Nota Segura, Identidade
    val onlyFavorites = MutableStateFlow(false)

    // Filtered entries for UI representation
    private val _filteredEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val filteredEntries: StateFlow<List<VaultEntry>> = _filteredEntries.asStateFlow()

    // Password Generator Configuration
    val genLength = MutableStateFlow(16)
    val genUseUppercase = MutableStateFlow(true)
    val genUseLowercase = MutableStateFlow(true)
    val genUseNumbers = MutableStateFlow(true)
    val genUseSymbols = MutableStateFlow(true)
    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VaultRepository(database.vaultEntryDao())
        otpRepository = OtpRepository(database.otpEntryDao())

        // Load Cloud Configurations
        CloudStorageManager.loadConfig(application)

        // Collect OTP entries for unified backup
        viewModelScope.launch {
            otpRepository.allEntries.collect { entries ->
                allOtpEntries.value = entries
            }
        }

        // Check if master password is set
        _isMasterPasswordSet.value = sharedPrefs.contains("master_password_hash")

        // Collect DB entries and update state flow
        viewModelScope.launch {
            repository.allEntries.collect { entries ->
                allEntries.value = entries
                filterEntries()
            }
        }

        // Apply filtering whenever queries or categories change
        viewModelScope.launch {
            combine(allEntries, searchQuery, selectedCategory, onlyFavorites) { entries, query, category, favorites ->
                var list = entries
                if (favorites) {
                    list = list.filter { it.isFavorite }
                }
                if (category != "Todos") {
                    list = list.filter { it.category.equals(category, ignoreCase = true) }
                }
                if (query.isNotEmpty()) {
                    list = list.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true)
                    }
                }
                list
            }.collect { filtered ->
                _filteredEntries.value = filtered
            }
        }

        generatePassword()

        // Auto-lock countdown timer
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            while (true) {
                kotlinx.coroutines.delay(1000)
                val elapsed = (System.currentTimeMillis() - lastInteractionTime) / 1000
                val remaining = maxOf(0, 300 - elapsed.toInt())
                secondsRemaining.value = remaining
                if (remaining == 0) {
                    if (_isUnlocked.value) {
                        lockVault()
                    }
                    // Reset lastInteractionTime so it doesn't loop triggers
                    lastInteractionTime = System.currentTimeMillis()
                    secondsRemaining.value = 300
                }
            }
        }
    }

    private fun filterEntries() {
        val query = searchQuery.value
        val category = selectedCategory.value
        val favorites = onlyFavorites.value
        val entries = allEntries.value

        var list = entries
        if (favorites) {
            list = list.filter { it.isFavorite }
        }
        if (category != "Todos") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
        _filteredEntries.value = list
    }

    // --- MASTER PASSWORD MANAGEMENT (DevSecOps Secure Hash PBKDF2 style simulation) ---
    fun setMasterPassword(password: String) {
        if (password.isBlank()) return
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        
        sharedPrefs.edit()
            .putString("master_password_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("master_password_hash", hash)
            .apply()

        _isMasterPasswordSet.value = true
        _isUnlocked.value = true
        resetInactivityTimer()
    }

    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = sharedPrefs.getString("master_password_hash", null) ?: return false
        val storedSaltBase64 = sharedPrefs.getString("master_password_salt", null) ?: return false
        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val computedHash = hashPassword(password, salt)

        val matches = MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )

        if (matches) {
            _isUnlocked.value = true
            resetInactivityTimer()
        }
        return matches
    }

    fun lockVault() {
        _isUnlocked.value = false
    }

    fun clearMasterPassword() {
        sharedPrefs.edit()
            .remove("master_password_hash")
            .remove("master_password_salt")
            .apply()
        _isMasterPasswordSet.value = false
        _isUnlocked.value = false
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        return try {
            val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(spec).encoded
            Base64.encodeToString(keyBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // --- VAULT ENTRIES CRUD ---
    fun addVaultEntry(
        title: String,
        usernameDecrypted: String,
        passwordDecrypted: String,
        url: String,
        notesDecrypted: String,
        category: String,
        isFavorite: Boolean
    ) {
        viewModelScope.launch {
            val (encryptedUser, userIv) = CryptoManager.encrypt(usernameDecrypted)
            val (encryptedPass, passIv) = CryptoManager.encrypt(passwordDecrypted)
            val (encryptedNotes, notesIv) = CryptoManager.encrypt(notesDecrypted)

            val newEntry = VaultEntry(
                title = title,
                usernameEncrypted = encryptedUser,
                usernameIv = userIv,
                passwordEncrypted = encryptedPass,
                passwordIv = passIv,
                url = url,
                notesEncrypted = encryptedNotes,
                notesIv = notesIv,
                category = category,
                isFavorite = isFavorite
            )
            repository.insertEntry(newEntry)
        }
    }

    fun updateVaultEntry(
        id: Long,
        title: String,
        usernameDecrypted: String,
        passwordDecrypted: String,
        url: String,
        notesDecrypted: String,
        category: String,
        isFavorite: Boolean
    ) {
        viewModelScope.launch {
            val (encryptedUser, userIv) = CryptoManager.encrypt(usernameDecrypted)
            val (encryptedPass, passIv) = CryptoManager.encrypt(passwordDecrypted)
            val (encryptedNotes, notesIv) = CryptoManager.encrypt(notesDecrypted)

            val existing = repository.getEntryById(id)
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    usernameEncrypted = encryptedUser,
                    usernameIv = userIv,
                    passwordEncrypted = encryptedPass,
                    passwordIv = passIv,
                    url = url,
                    notesEncrypted = encryptedNotes,
                    notesIv = notesIv,
                    category = category,
                    isFavorite = isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateEntry(updated)
            }
        }
    }

    fun toggleFavorite(entry: VaultEntry) {
        viewModelScope.launch {
            val updated = entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis())
            repository.updateEntry(updated)
        }
    }

    fun deleteVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun decryptField(encrypted: String, iv: String): String {
        if (encrypted.isEmpty() || iv.isEmpty()) return ""
        return CryptoManager.decrypt(encrypted, iv)
    }

    // --- PASSWORD STRENGTH & HEALTH ANALYZER ---
    fun getSecurityHealthReport(): SecurityReport {
        val entries = allEntries.value.filter { it.category == "Login" || it.passwordEncrypted.isNotEmpty() }
        var total = entries.size
        var weakCount = 0
        var strongCount = 0
        var reusedCount = 0

        // Map to find reused passwords
        val passwordCountMap = mutableMapOf<String, Int>()
        val decryptedPasswords = entries.map { entry ->
            val pass = decryptField(entry.passwordEncrypted, entry.passwordIv)
            passwordCountMap[pass] = (passwordCountMap[pass] ?: 0) + 1
            pass
        }

        for (pass in decryptedPasswords) {
            val strength = evaluatePasswordStrength(pass)
            if (strength == PasswordStrength.WEAK || strength == PasswordStrength.VERY_WEAK) {
                weakCount++
            } else if (strength == PasswordStrength.STRONG || strength == PasswordStrength.VERY_STRONG) {
                strongCount++
            }
            if ((passwordCountMap[pass] ?: 0) > 1 && pass.isNotEmpty()) {
                reusedCount++
            }
        }

        val safetyScore = if (total == 0) 100 else {
            val penaltyForWeak = (weakCount * 25)
            val penaltyForReused = (reusedCount * 15)
            val score = 100 - (penaltyForWeak + penaltyForReused) / total
            maxOf(0, minOf(100, score))
        }

        return SecurityReport(
            totalPasswords = total,
            weakPasswords = weakCount,
            strongPasswords = strongCount,
            reusedPasswords = reusedCount,
            safetyScore = safetyScore
        )
    }

    fun evaluatePasswordStrength(pass: String): PasswordStrength {
        if (pass.isEmpty()) return PasswordStrength.VERY_WEAK
        if (pass.length < 6) return PasswordStrength.VERY_WEAK
        
        var score = 0
        if (pass.length >= 8) score++
        if (pass.length >= 12) score++
        if (pass.any { it.isUpperCase() }) score++
        if (pass.any { it.isLowerCase() }) score++
        if (pass.any { it.isDigit() }) score++
        if (pass.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> PasswordStrength.VERY_WEAK
            score == 3 -> PasswordStrength.WEAK
            score == 4 -> PasswordStrength.MEDIUM
            score == 5 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    // --- PASSWORD GENERATOR ---
    fun generatePassword() {
        val length = genLength.value
        val upper = genUseUppercase.value
        val lower = genUseLowercase.value
        val digits = genUseNumbers.value
        val symbols = genUseSymbols.value

        val upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerChars = "abcdefghijklmnopqrstuvwxyz"
        val digitChars = "0123456789"
        val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        var charPool = ""
        val mandatoryPool = mutableListOf<Char>()

        if (upper) {
            charPool += upperChars
            mandatoryPool.add(upperChars.random())
        }
        if (lower) {
            charPool += lowerChars
            mandatoryPool.add(lowerChars.random())
        }
        if (digits) {
            charPool += digitChars
            mandatoryPool.add(digitChars.random())
        }
        if (symbols) {
            charPool += symbolChars
            mandatoryPool.add(symbolChars.random())
        }

        if (charPool.isEmpty()) {
            _generatedPassword.value = ""
            return
        }

        val random = SecureRandom()
        val generatedSb = java.lang.StringBuilder()
        
        // Put mandatory characters first to ensure complexity
        generatedSb.append(mandatoryPool.joinToString(""))

        val remainingLength = length - mandatoryPool.size
        for (i in 0 until remainingLength) {
            val index = random.nextInt(charPool.length)
            generatedSb.append(charPool[index])
        }

        // Shuffle generated characters to avoid predictable patterns
        val charList = generatedSb.toString().toList().shuffled(random)
        _generatedPassword.value = charList.joinToString("")
    }

    // --- PORTABLE ENCRYPTED EXPORT & IMPORT (AES-256-GCM with PBKDF2) ---

    fun generateEncryptedBackup(password: String): String? {
        val verified = verifyMasterPassword(password)
        if (!verified) return null

        val plaintext = SecureBackupManager.createPlaintextBackup(
            otpEntries = allOtpEntries.value,
            vaultEntries = allEntries.value,
            otpDecryptor = { encrypted, iv -> decryptField(encrypted, iv) },
            vaultDecryptor = { encrypted, iv -> decryptField(encrypted, iv) }
        )

        return SecureBackupManager.encryptBackup(plaintext, password)
    }

    fun restoreFromEncryptedBackup(backupJson: String, password: String): Boolean {
        val decrypted = SecureBackupManager.decryptBackup(backupJson, password) ?: return false
        val parsed = SecureBackupManager.parseDecryptedBackup(decrypted) ?: return false

        viewModelScope.launch {
            // Import OTP entries
            for (otp in parsed.otpEntries) {
                val (encryptedSecret, iv) = CryptoManager.encrypt(otp.secret)
                otpRepository.insertEntry(
                    com.example.data.OtpEntry(
                        displayName = otp.displayName,
                        issuer = otp.issuer,
                        label = otp.label,
                        encryptedSecret = encryptedSecret,
                        iv = iv,
                        algorithm = otp.algorithm,
                        digits = otp.digits,
                        period = otp.period,
                        displayOrder = otp.displayOrder
                    )
                )
            }

            // Import Vault entries
            for (vault in parsed.vaultEntries) {
                val (encryptedUser, userIv) = CryptoManager.encrypt(vault.username)
                val (encryptedPass, passIv) = CryptoManager.encrypt(vault.password)
                val (encryptedNotes, notesIv) = CryptoManager.encrypt(vault.notes)

                repository.insertEntry(
                    VaultEntry(
                        title = vault.title,
                        usernameEncrypted = encryptedUser,
                        usernameIv = userIv,
                        passwordEncrypted = encryptedPass,
                        passwordIv = passIv,
                        url = vault.url,
                        notesEncrypted = encryptedNotes,
                        notesIv = notesIv,
                        category = vault.category,
                        isFavorite = vault.isFavorite,
                        createdAt = vault.createdAt,
                        updatedAt = vault.updatedAt
                    )
                )
            }
        }
        return true
    }

    // --- CLOUD STORAGE MANAGEMENT ---
    val cloudConfig: StateFlow<CloudConfig> = CloudStorageManager.currentConfig
    val cloudSyncStatus: StateFlow<CloudStorageManager.SyncStatus> = CloudStorageManager.syncStatus

    fun updateCloudConfig(config: CloudConfig) {
        CloudStorageManager.saveConfig(getApplication(), config)
    }

    fun clearCloudSyncStatus() {
        CloudStorageManager.clearStatus()
    }

    fun uploadBackupToCloud(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val backupJson = generateEncryptedBackup(password)
        if (backupJson == null) {
            onError("Senha Master inválida ou erro na criptografia.")
            return
        }
        viewModelScope.launch {
            val success = CloudStorageManager.uploadBackup(getApplication(), backupJson)
            if (success) {
                onSuccess()
            } else {
                val status = CloudStorageManager.syncStatus.value
                val errMsg = if (status is CloudStorageManager.SyncStatus.Error) status.message else "Erro desconhecido na sincronização em nuvem."
                onError(errMsg)
            }
        }
    }

    fun downloadBackupFromCloud(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val backupJson = CloudStorageManager.downloadBackup(getApplication())
            if (backupJson == null) {
                val status = CloudStorageManager.syncStatus.value
                val errMsg = if (status is CloudStorageManager.SyncStatus.Error) status.message else "Erro ao baixar arquivo de backup ou integração desativada."
                onError(errMsg)
                return@launch
            }

            // High-fidelity simulation backup helper
            val finalBackup = if (backupJson == "SIMULATED_ENCRYPTED_BACKUP_CONTENT") {
                val plaintext = SecureBackupManager.createPlaintextBackup(
                    otpEntries = allOtpEntries.value,
                    vaultEntries = allEntries.value,
                    otpDecryptor = { encrypted, iv -> decryptField(encrypted, iv) },
                    vaultDecryptor = { encrypted, iv -> decryptField(encrypted, iv) }
                )
                SecureBackupManager.encryptBackup(plaintext, password)
            } else {
                backupJson
            }

            val success = restoreFromEncryptedBackup(finalBackup, password)
            if (success) {
                onSuccess()
            } else {
                onError("Falha na descriptografia. Verifique a senha informada.")
            }
        }
    }
}

// Data models for safety report
data class SecurityReport(
    val totalPasswords: Int,
    val weakPasswords: Int,
    val strongPasswords: Int,
    val reusedPasswords: Int,
    val safetyScore: Int
)

enum class PasswordStrength(val label: String, val colorHex: Long) {
    VERY_WEAK("Muito Fraca", 0xFFEF4444), // Danger Red
    WEAK("Fraca", 0xFFF59E0B),       // Alert Orange
    MEDIUM("Média", 0xFFFFC107),      // Amber
    STRONG("Forte", 0xFF10B981),      // Success Green
    VERY_STRONG("Excelente", 0xFF06B6D4) // Neon Cyan
}
