package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "otp_entries")
data class OtpEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val issuer: String,
    val label: String,
    val encryptedSecret: String,
    val iv: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val displayOrder: Int = 0
)
