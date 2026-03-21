package com.example.yuxiaofy

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.io.File

@UnstableApi
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    companion object {
        private var cache: SimpleCache? = null

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
    }

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

        // Cache DataSource - lưu nhạc xuống máy khi phát
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(this))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}