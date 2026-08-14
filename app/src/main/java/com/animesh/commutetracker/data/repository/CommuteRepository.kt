package com.animesh.commutetracker.data.repository

import com.animesh.commutetracker.data.database.CommuteDao
import com.animesh.commutetracker.data.model.CommuteRecord
import com.animesh.commutetracker.data.model.DiagnosticLog
import com.animesh.commutetracker.data.model.TransportMode
import kotlinx.coroutines.flow.Flow

class CommuteRepository(private val commuteDao: CommuteDao) {

    val allRecords: Flow<List<CommuteRecord>> = commuteDao.getAllRecords()

    val pendingDetailsRecord: Flow<CommuteRecord?> = commuteDao.getPendingDetailsRecord()

    val recentLogs: Flow<List<DiagnosticLog>> = commuteDao.getRecentLogs()

    suspend fun logEvent(event: String, message: String = "") {
        commuteDao.insertLog(DiagnosticLog(timestamp = System.currentTimeMillis(), event = event, message = message))
    }

    suspend fun clearLogs() = commuteDao.clearLogs()

    suspend fun getRecordById(id: Long): CommuteRecord? = commuteDao.getRecordById(id)

    fun getRecordsByDate(date: String): Flow<List<CommuteRecord>> = commuteDao.getRecordsByDate(date)

    suspend fun insertRecord(record: CommuteRecord): Long = commuteDao.insertRecord(record)

    suspend fun updateRecord(record: CommuteRecord) = commuteDao.updateRecord(record)

    suspend fun deleteRecord(record: CommuteRecord) = commuteDao.deleteRecord(record)

    suspend fun clearHistory() = commuteDao.clearHistory()

    suspend fun getRecentCostsForTransport(mode: TransportMode): List<Int> = 
        commuteDao.getRecentCostsForTransport(mode)

    suspend fun getAllRecordsSync(): List<CommuteRecord> = commuteDao.getAllRecordsSync()
}
