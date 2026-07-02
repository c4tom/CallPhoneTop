package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {
    @Query("SELECT * FROM vault_entries ORDER BY isFavorite DESC, title ASC")
    fun getAllEntries(): Flow<List<VaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntry): Long

    @Update
    suspend fun updateEntry(entry: VaultEntry)

    @Delete
    suspend fun deleteEntry(entry: VaultEntry)

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): VaultEntry?
}
