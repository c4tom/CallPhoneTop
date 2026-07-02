package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val vaultEntryDao: VaultEntryDao) {
    val allEntries: Flow<List<VaultEntry>> = vaultEntryDao.getAllEntries()

    suspend fun insertEntry(entry: VaultEntry): Long {
        return vaultEntryDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: VaultEntry) {
        vaultEntryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: VaultEntry) {
        vaultEntryDao.deleteEntry(entry)
    }

    suspend fun getEntryById(id: Long): VaultEntry? {
        return vaultEntryDao.getEntryById(id)
    }
}
