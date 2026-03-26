package database

import androidx.room.*

@Dao
interface DownloadedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: DownloadedSong)

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    suspend fun getAllDownloadedSongs(): List<DownloadedSong>

    @Query("SELECT * FROM downloaded_songs WHERE id = :songId")
    suspend fun getDownloadedSongById(songId: String): DownloadedSong?

    @Delete
    suspend fun delete(song: DownloadedSong)
}
