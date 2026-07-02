package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpEntryDao {
    @Query("SELECT * FROM otp_entries ORDER BY displayOrder ASC, id ASC")
    fun getAllEntries(): Flow<List<OtpEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: OtpEntry): Long

    @Update
    suspend fun updateEntry(entry: OtpEntry)

    @Delete
    suspend fun deleteEntry(entry: OtpEntry)

    @Query("SELECT COALESCE(MAX(displayOrder), 0) FROM otp_entries")
    suspend fun getMaxOrder(): Int

    @Transaction
    suspend fun insertWithMaxOrder(entry: OtpEntry): Long {
        val maxOrder = getMaxOrder()
        val newEntry = entry.copy(displayOrder = maxOrder + 1)
        return insertEntry(newEntry)
    }
}
