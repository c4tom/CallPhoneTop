package com.example.data

import kotlinx.coroutines.flow.Flow

class OtpRepository(private val otpEntryDao: OtpEntryDao) {
    val allEntries: Flow<List<OtpEntry>> = otpEntryDao.getAllEntries()

    suspend fun insertEntry(entry: OtpEntry): Long {
        return otpEntryDao.insertWithMaxOrder(entry)
    }

    suspend fun updateEntry(entry: OtpEntry) {
        otpEntryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: OtpEntry) {
        otpEntryDao.deleteEntry(entry)
    }
}
