package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val note: String = "",
    val avatarColor: Int = 0, // A color code index for beautiful custom background circles
    val isFavorite: Boolean = false
)
