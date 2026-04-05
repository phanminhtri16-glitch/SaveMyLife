package com.example.yuxiaofy

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.Player
import com.google.firebase.auth.FirebaseAuth
import database.AppDatabase
import database.ListenHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File

@UnstableApi
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var lastSavedSongId: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private var cache: SimpleCache? = null

        @OptIn(UnstableApi::class)
        fun getCache(service: MusicService): SimpleCache {
            if (cache == null) {
                val cacheDir = File(service.cacheDir, "media_cache")
                val cacheSize = 500L * 1024 * 1024 // 500MB cache
                val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
                val dbProvider = StandaloneDatabaseProvider(service)
                cache = SimpleCache(cacheDir, evictor, dbProvider)
            }
            return cache!!
        }

        fun releaseCache() {
            cache?.release()
            cache = null
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Load control - buffer toàn bộ bài hát
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                60_000,    // minBufferMs - 1 phút
                600_000,   // maxBufferMs - 10 phút (buffer cả bài)
                1_000,     // bufferForPlaybackMs
                2_000      // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // HTTP DataSource với timeout cao
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)

        // SỬ DỤNG DefaultDataSource.Factory để hỗ trợ cả file://, content://, v.v...
        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Cache DataSource - lưu nhạc xuống máy khi phát
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(this))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) saveToListenHistory()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (player.isPlaying) saveToListenHistory()
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        releaseCache()
        super.onDestroy()
    }

    private fun saveToListenHistory() {
        val prefs = applicationContext.getSharedPreferences("yuxiaofy_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("privacy_listen_history", true)) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentItem = player.currentMediaItem ?: return
        val songId = currentItem.mediaId
        
        if (songId == lastSavedSongId || songId.isEmpty()) return
        
        val meta = currentItem.mediaMetadata
        val title = meta.title?.toString() ?: "Unknown"
        val artist = meta.artist?.toString() ?: "Unknown"
        val coverUrl = meta.artworkUri?.toString() ?: ""
        val audioUrl = currentItem.localConfiguration?.uri?.toString() ?: ""

        if (title == "Unknown") return

        lastSavedSongId = songId

        serviceScope.launch {
            try {
                val dbLocal = AppDatabase.getDatabase(this@MusicService)
                dbLocal.listenHistoryDao().addToHistory(
                    ListenHistory(
                        userId = uid,
                        songId = songId,
                        title = title,
                        artist = artist,
                        coverUrl = coverUrl,
                        audioUrl = audioUrl
                    )
                )
            } catch (e: Exception) {
                Log.e("MusicService", "Error saving history: ${e.message}")
            }
        }
    }
}