package com.animesh.commutetracker.data.database

import androidx.room.*
import com.animesh.commutetracker.data.model.CommuteMode
import com.animesh.commutetracker.data.model.CommuteRecord
import com.animesh.commutetracker.data.model.CommuteWithModes
import com.animesh.commutetracker.data.model.DiagnosticLog
import com.animesh.commutetracker.data.model.TransportMode
import kotlinx.coroutines.flow.Flow

@Dao
interface CommuteDao {
    @Transaction
    @Query("SELECT * FROM commute_records ORDER BY arrivalTimestamp DESC")
    fun getAllRecords(): Flow<List<CommuteWithModes>>

    @Transaction
    @Query("SELECT * FROM commute_records WHERE date = :date ORDER BY arrivalTimestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<CommuteWithModes>>

    @Transaction
    @Query("SELECT * FROM commute_records WHERE id = :id")
    suspend fun getRecordById(id: Long): CommuteWithModes?

    @Transaction
    @Query("SELECT * FROM commute_records WHERE status = 'PENDING_DETAILS' LIMIT 1")
    fun getPendingDetailsRecord(): Flow<CommuteWithModes?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CommuteRecord): Long

    @Insert
    suspend fun insertModes(modes: List<CommuteMode>)

    @Update
    suspend fun updateRecord(record: CommuteRecord)

    @Query("DELETE FROM commute_modes WHERE commuteId = :commuteId")
    suspend fun deleteModesForCommute(commuteId: Long)

    @Delete
    suspend fun deleteRecord(record: CommuteRecord)

    @Query("DELETE FROM commute_records")
    suspend fun clearHistory()

    @Query("SELECT cost FROM commute_modes WHERE transportMode = :mode AND cost > 0 GROUP BY cost ORDER BY MAX(modeId) DESC LIMIT 4")
    suspend fun getRecentCostsForTransport(mode: TransportMode): List<Int>

    @Transaction
    @Query("SELECT * FROM commute_records")
    suspend fun getAllRecordsSync(): List<CommuteWithModes>

    // Diagnostic Logs
    @Insert
    suspend fun insertLog(log: DiagnosticLog)

    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<DiagnosticLog>>

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}
