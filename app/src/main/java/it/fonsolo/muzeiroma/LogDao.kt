package it.fonsolo.muzeiroma

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM app_logs WHERE id NOT IN (SELECT id FROM app_logs ORDER BY timestamp DESC LIMIT 200)")
    suspend fun pruneLogs()
}
