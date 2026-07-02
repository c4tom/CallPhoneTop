package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val usernameEncrypted: String,
    val usernameIv: String,
    val passwordEncrypted: String,
    val passwordIv: String,
    val url: String = "",
    val notesEncrypted: String = "",
    val notesIv: String = "",
    val category: String = "Login", // Login, Cartão, Nota Segura, Identidade
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
