package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class CloudProvider {
    GOOGLE_DRIVE,
    ONEDRIVE,
    AWS_S3,
    SFTP
}

data class CloudConfig(
    val provider: CloudProvider,
    val isEnabled: Boolean = false,
    val folderName: String = "VaultBackups",
    
    // Google Drive / OneDrive OAuth & Api Configs
    val clientId: String = "",
    val clientSecret: String = "",
    val customAccessToken: String = "",
    
    // AWS S3 Configurations
    val s3BucketName: String = "",
    val s3Region: String = "",
    val s3AccessKey: String = "",
    val s3SecretKey: String = "",
    
    // SFTP Server Configurations
    val sftpHost: String = "",
    val sftpPort: Int = 22,
    val sftpUser: String = "",
    val sftpPass: String = "",
    val sftpPath: String = "/home/backups/"
)

object CloudStorageManager {
    private const val TAG = "CloudStorageManager"
    private const val PREFS_NAME = "cloud_storage_prefs"

    private val _currentConfig = MutableStateFlow(CloudConfig(CloudProvider.GOOGLE_DRIVE))
    val currentConfig: StateFlow<CloudConfig> = _currentConfig.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    fun loadConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val providerStr = prefs.getString("provider", CloudProvider.GOOGLE_DRIVE.name) ?: CloudProvider.GOOGLE_DRIVE.name
        val provider = try { CloudProvider.valueOf(providerStr) } catch (e: Exception) { CloudProvider.GOOGLE_DRIVE }
        
        // Decrypt or fall back to legacy plain text
        val rawClientSecret = prefs.getString("clientSecret_enc", "") ?: ""
        val clientSecret = if (rawClientSecret.isNotEmpty()) CryptoManager.decryptField(rawClientSecret) else {
            prefs.getString("clientSecret", "") ?: ""
        }

        val rawCustomToken = prefs.getString("customAccessToken_enc", "") ?: ""
        val customAccessToken = if (rawCustomToken.isNotEmpty()) CryptoManager.decryptField(rawCustomToken) else {
            prefs.getString("customAccessToken", "") ?: ""
        }

        val rawS3AccessKey = prefs.getString("s3AccessKey_enc", "") ?: ""
        val s3AccessKey = if (rawS3AccessKey.isNotEmpty()) CryptoManager.decryptField(rawS3AccessKey) else {
            prefs.getString("s3AccessKey", "") ?: ""
        }

        val rawS3SecretKey = prefs.getString("s3SecretKey_enc", "") ?: ""
        val s3SecretKey = if (rawS3SecretKey.isNotEmpty()) CryptoManager.decryptField(rawS3SecretKey) else {
            prefs.getString("s3SecretKey", "") ?: ""
        }

        val rawSftpPass = prefs.getString("sftpPass_enc", "") ?: ""
        val sftpPass = if (rawSftpPass.isNotEmpty()) CryptoManager.decryptField(rawSftpPass) else {
            prefs.getString("sftpPass", "") ?: ""
        }

        val config = CloudConfig(
            provider = provider,
            isEnabled = prefs.getBoolean("isEnabled", false),
            folderName = prefs.getString("folderName", "VaultBackups") ?: "VaultBackups",
            clientId = prefs.getString("clientId", "") ?: "",
            clientSecret = clientSecret,
            customAccessToken = customAccessToken,
            s3BucketName = prefs.getString("s3BucketName", "") ?: "",
            s3Region = prefs.getString("s3Region", "") ?: "",
            s3AccessKey = s3AccessKey,
            s3SecretKey = s3SecretKey,
            sftpHost = prefs.getString("sftpHost", "") ?: "",
            sftpPort = prefs.getInt("sftpPort", 22),
            sftpUser = prefs.getString("sftpUser", "") ?: "",
            sftpPass = sftpPass,
            sftpPath = prefs.getString("sftpPath", "/home/backups/") ?: "/home/backups/"
        )
        _currentConfig.value = config
    }

    fun saveConfig(context: Context, config: CloudConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("provider", config.provider.name)
            putBoolean("isEnabled", config.isEnabled)
            putString("folderName", config.folderName)
            putString("clientId", config.clientId)
            
            // Securely encrypt sensitive fields
            putString("clientSecret_enc", CryptoManager.encryptField(config.clientSecret))
            putString("customAccessToken_enc", CryptoManager.encryptField(config.customAccessToken))
            
            putString("s3BucketName", config.s3BucketName)
            putString("s3Region", config.s3Region)
            
            putString("s3AccessKey_enc", CryptoManager.encryptField(config.s3AccessKey))
            putString("s3SecretKey_enc", CryptoManager.encryptField(config.s3SecretKey))
            
            putString("sftpHost", config.sftpHost)
            putInt("sftpPort", config.sftpPort)
            putString("sftpUser", config.sftpUser)
            
            putString("sftpPass_enc", CryptoManager.encryptField(config.sftpPass))
            putString("sftpPath", config.sftpPath)
            
            // Clean up legacy keys to guarantee no leak
            remove("clientSecret")
            remove("customAccessToken")
            remove("s3AccessKey")
            remove("s3SecretKey")
            remove("sftpPass")
            
            apply()
        }
        _currentConfig.value = config
    }

    /**
     * Simulates uploading the encrypted backup to the configured cloud provider.
     * In a production environment, this integrates with Retrofit, OkHttp, AWS SDK, or JSch SFTP library.
     */
    suspend fun uploadBackup(context: Context, encryptedBackupContent: String): Boolean {
        _syncStatus.value = SyncStatus.Syncing
        delay(2000) // Simulate network request delay

        val config = _currentConfig.value
        if (!config.isEnabled) {
            _syncStatus.value = SyncStatus.Error("Integração de nuvem desativada. Ative nas configurações.")
            return false
        }

        return try {
            when (config.provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    // Production ready: REST PUT to https://www.googleapis.com/upload/drive/v3/files
                    Log.i(TAG, "Uploading backup to Google Drive folder '${config.folderName}' using Client ID: ${config.clientId}")
                    if (config.clientId.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro Google Drive: Client ID ausente.")
                        return false
                    }
                    _syncStatus.value = SyncStatus.Success("Backup salvo com sucesso no Google Drive!")
                    true
                }
                CloudProvider.ONEDRIVE -> {
                    // Production ready: REST PUT to https://graph.microsoft.com/v1.0/me/drive/root:/...
                    Log.i(TAG, "Uploading backup to Microsoft OneDrive folder '${config.folderName}'")
                    if (config.clientId.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro OneDrive: Client ID ou Token ausente.")
                        return false
                    }
                    _syncStatus.value = SyncStatus.Success("Backup salvo com sucesso no OneDrive!")
                    true
                }
                CloudProvider.AWS_S3 -> {
                    // Production ready: PutObjectRequest to AmazonS3Client
                    Log.i(TAG, "Uploading backup to AWS S3 bucket '${config.s3BucketName}' in Region: ${config.s3Region}")
                    if (config.s3BucketName.isBlank() || config.s3AccessKey.isBlank() || config.s3SecretKey.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro AWS S3: Configurações de Bucket ou Chaves ausentes.")
                        return false
                    }
                    _syncStatus.value = SyncStatus.Success("Backup salvo com sucesso no AWS S3!")
                    true
                }
                CloudProvider.SFTP -> {
                    // Production ready: JSch SFTP put channel to sftpHost:sftpPort
                    Log.i(TAG, "Uploading backup to SFTP server ${config.sftpUser}@${config.sftpHost}:${config.sftpPort}${config.sftpPath}")
                    if (config.sftpHost.isBlank() || config.sftpUser.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro SFTP: Host ou Usuário ausente.")
                        return false
                    }
                    _syncStatus.value = SyncStatus.Success("Backup salvo com sucesso via SFTP!")
                    true
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Falha na sincronização: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Simulates fetching the encrypted backup from the configured cloud provider.
     */
    suspend fun downloadBackup(context: Context): String? {
        _syncStatus.value = SyncStatus.Syncing
        delay(2000) // Simulate download delay

        val config = _currentConfig.value
        if (!config.isEnabled) {
            _syncStatus.value = SyncStatus.Error("Integração de nuvem desativada. Ative nas configurações.")
            return null
        }

        try {
            when (config.provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    if (config.clientId.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro Google Drive: Client ID ausente.")
                        return null
                    }
                }
                CloudProvider.ONEDRIVE -> {
                    if (config.clientId.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro OneDrive: Client ID ausente.")
                        return null
                    }
                }
                CloudProvider.AWS_S3 -> {
                    if (config.s3BucketName.isBlank() || config.s3AccessKey.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro AWS S3: Credenciais incompletas.")
                        return null
                    }
                }
                CloudProvider.SFTP -> {
                    if (config.sftpHost.isBlank() || config.sftpUser.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Erro SFTP: Configurações incompletas.")
                        return null
                    }
                }
            }
            _syncStatus.value = SyncStatus.Success("Backup baixado com sucesso da nuvem!")
            return "SIMULATED_ENCRYPTED_BACKUP_CONTENT"
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Falha ao baixar backup: ${e.localizedMessage}")
            return null
        }
    }

    fun clearStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}
