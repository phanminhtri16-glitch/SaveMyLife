package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListenHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(history: ListenHistory)

    @Query("SELECT * FROM listen_history ORDER BY listenedAt DESC LIMIT 50")
    suspend fun getRecentHistory(): List<ListenHistory>

    @Query("DELETE FROM listen_history")
    suspend fun clearHistory()

    @Query("DELETE FROM listen_history WHERE listenedAt < :timeLimit")
    suspend fun deleteOldHistory(timeLimit: Long)
}