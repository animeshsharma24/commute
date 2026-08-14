package com.animesh.commutetracker.data.database

import androidx.room.*
import com.animesh.commutetracker.data.model.CommuteRecord
import com.animesh.commutetracker.data.model.DiagnosticLog
import com.animesh.commutetracker.data.model.TransportMode
import kotlinx.coroutines.flow.Flow

@Dao
interface CommuteDao {
    @Query("SELECT * FROM commute_records ORDER BY arrivalTimestamp DESC")
    fun getAllRecords(): Flow<List<CommuteRecord>>

    @Query("SELECT * FROM commute_records WHERE date = :date ORDER BY arrivalTimestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<CommuteRecord>>

    @Query("SELECT * FROM commute_records WHERE id = :id")
    suspend fun getRecordById(id: Long): CommuteRecord?

    @Query("SELECT * FROM commute_records WHERE status = 'PENDING_DETAILS' LIMIT 1")
    fun getPendingDetailsRecord(): Flow<CommuteRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CommuteRecord): Long

    @Update
    suspend fun updateRecord(record: CommuteRecord)

    @Delete
    suspend fun deleteRecord(record: CommuteRecord)

    @Query("DELETE FROM commute_records")
    suspend fun clearHistory()

    @Query("SELECT DISTINCT cost FROM commute_records WHERE transportMode = :mode AND cost IS NOT NULL AND status = 'COMPLETED' ORDER BY arrivalTimestamp DESC LIMIT 4")
    suspend fun getRecentCostsForTransport(mode: TransportMode): List<Int>

    @Query("SELECT * FROM commute_records")
    suspend fun getAllRecordsSync(): List<CommuteRecord>

    // Diagnostic Logs
    @Insert
    suspend fun insertLog(log: DiagnosticLog)

    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<DiagnosticLog>>

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}
