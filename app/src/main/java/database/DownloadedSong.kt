package database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSong(
    @PrimaryKey val id: String, // Same as Firestore Song ID
    val title: String,
    val artist: String,
    val audioUrl: String, // Original URL
    val localPath: String, // Path to the downloaded file on device
    val coverUrl: String,
    val duration: String = "",
    val downloadedAt: Long = System.currentTimeMillis()
)
