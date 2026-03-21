package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListenHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(history: ListenHistory)

    // Lấy lịch sử theo userId
    @Query("SELECT * FROM listen_history WHERE userId = :userId ORDER BY listenedAt DESC LIMIT 50")
    suspend fun getHistoryByUser(userId: String): List<ListenHistory>

    // Xóa lịch sử của 1 user
    @Query("DELETE FROM listen_history WHERE userId = :userId")
    suspend fun clearHistoryByUser(userId: String)

    // Xóa lịch sử cũ của 1 user
    @Query("DELETE FROM listen_history WHERE userId = :userId AND listenedAt < :timeLimit")
    suspend fun deleteOldHistoryByUser(userId: String, timeLimit: Long)
}