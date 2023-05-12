package de.lmu.personalflashcards.model

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "log")
class LogEntry(val timestamp: Long, val type: String, val values: String = "", val quizID: Long = -1) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var synced: Boolean = false
}

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(logEntry: LogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultiple(logEntries: List<LogEntry>)

    @Query("SELECT CAST(COUNT(*) AS REAL) / MAX(CAST(COUNT(CASE WHEN [values] LIKE \"correct%\" THEN 1.0 END) AS REAL), 1.0) FROM log WHERE type = \"answer_selected\"")
    fun averageAttempts(): LiveData<Float>

    @Query("SELECT * FROM log WHERE type = \"training_started\" ORDER BY timestamp DESC LIMIT 1")
    fun getLastTrainingDate(): LiveData<LogEntry>

    @Query("SELECT * FROM log WHERE synced = 0")
    suspend fun getUnsyncedEntries(): List<LogEntry>

//    @Query("SELECT COUNT(*) FROM log WHERE [values] LIKE \"correct%\" AND timestamp > ")
//    suspend fun getTotalQuizzes
}