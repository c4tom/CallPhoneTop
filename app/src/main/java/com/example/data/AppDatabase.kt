package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OtpEntry::class, VaultEntry::class, Contact::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun otpEntryDao(): OtpEntryDao
    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "otp_authenticator_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
