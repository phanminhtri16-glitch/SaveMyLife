package database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listen_history")
data class ListenHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,        // ← phân biệt theo user
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    val listenedAt: Long = System.currentTimeMillis()
)