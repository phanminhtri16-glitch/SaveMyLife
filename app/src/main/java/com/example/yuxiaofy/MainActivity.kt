package com.example.yuxiaofy

import android.animation.ObjectAnimator
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import database.AppDatabase
import database.DownloadedSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class LocalSong(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String,
    val duration: String = "3:00"
)

@androidx.media3.common.util.UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var rotateAnimator: ObjectAnimator
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var playBtn: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvArtistName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnHeart: ImageView
    private lateinit var coverArt: ImageView
    private lateinit var rvPlaylist: RecyclerView
    private var isFavorite = false

    private lateinit var btnLyrics: ImageView
    private lateinit var btnDownload: ImageView
    private lateinit var scrollLyrics: ScrollView
    private lateinit var tvLyrics: TextView
    private lateinit var cardCoverArt: CardView
    private var isLyricsVisible = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val handler = Handler(Looper.getMainLooper())
    private var songId = ""
    private var audioUrl = "" 
    private var coverArtUrl = ""
    private var currentSongTitle = ""
    private var currentArtist = ""
    private var currentDuration = ""
    private var isLocalMode = false

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val playlistSongs = mutableListOf<LocalSong>()
    private lateinit var musicAdapter: MusicPlayerAdapter

    private val progressRunnable = object : Runnable {
        override fun run() {
            controller?.let { c ->
                if (c.isPlaying) {
                    seekBar.progress = c.currentPosition.toInt()
                    updateTimeDisplay((c.currentPosition / 1000).toInt())
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        bindViews()
        setupSongInfo()
        setupCoverArtAnimation()
        setupControls()
        setupSeekBar()
        animateEntrance()
        setupMediaController()
    }

    private fun bindViews() {
        playBtn = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        tvSongTitle = findViewById(R.id.tvSongTitle)
        tvArtistName = findViewById(R.id.tvArtistName)
        btnBack = findViewById(R.id.btnBack)
        btnHeart = findViewById(R.id.btnHeart)
        coverArt = findViewById(R.id.cover_art)
        rvPlaylist = findViewById(R.id.rvPlaylist)
        btnLyrics = findViewById(R.id.btnLyrics)
        btnDownload = findViewById(R.id.btnDownload)
        scrollLyrics = findViewById(R.id.scrollLyrics)
        tvLyrics = findViewById(R.id.tvLyrics)
        cardCoverArt = findViewById(R.id.cardCoverArt)
    }

    private fun setupSongInfo() {
        songId = intent.getStringExtra("SONG_ID") ?: ""
        currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Unknown"
        currentArtist = intent.getStringExtra("SONG_ARTIST") ?: "Unknown"
        audioUrl = intent.getStringExtra("SONG_AUDIO_URL") ?: ""
        coverArtUrl = intent.getStringExtra("SONG_COVER_URL") ?: intent.getStringExtra("SONG_COVER_ART_URL") ?: ""
        currentDuration = intent.getStringExtra("SONG_DURATION") ?: "0:00"
        isLocalMode = intent.getBooleanExtra("IS_LOCAL", false)

        tvSongTitle.text = currentSongTitle
        tvArtistName.text = currentArtist
        tvTotalTime.text = currentDuration
        tvCurrentTime.text = "0:00"

        if (coverArtUrl.isNotEmpty()) {
            Glide.with(this).load(coverArtUrl).placeholder(R.drawable.bg_glow_circle).into(coverArt)
        }
        checkFavoriteStatus()
        checkDownloadStatus()
    }

    private fun setupCoverArtAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(coverArt, "rotation", 0f, 360f).apply {
            duration = 12000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        playBtn.setImageResource(android.R.drawable.ic_media_pause)
                        if (rotateAnimator.isPaused) rotateAnimator.resume() else rotateAnimator.start()
                        handler.post(progressRunnable)
                    } else {
                        playBtn.setImageResource(android.R.drawable.ic_media_play)
                        rotateAnimator.pause()
                        handler.removeCallbacks(progressRunnable)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        seekBar.max = controller?.duration?.toInt() ?: 0
                        val totalSecs = (controller?.duration ?: 0) / 1000
                        tvTotalTime.text = String.format(Locale.getDefault(), "%d:%02d", totalSecs / 60, totalSecs % 60)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.let { item ->
                        songId = item.mediaId
                        val found = playlistSongs.find { it.id == songId }
                        if (found != null) {
                            currentSongTitle = found.title
                            currentArtist = found.artist
                            audioUrl = found.audioUrl
                            coverArtUrl = found.coverUrl
                            currentDuration = found.duration

                            tvSongTitle.text = currentSongTitle
                            tvArtistName.text = currentArtist
                            if (coverArtUrl.isNotEmpty()) {
                                Glide.with(this@MainActivity).load(coverArtUrl).placeholder(R.drawable.bg_glow_circle).into(coverArt)
                            }
                        }
                        
                        val currentIndex = controller?.currentMediaItemIndex ?: 0
                        if (::musicAdapter.isInitialized) musicAdapter.updateCurrentPos(currentIndex)
                        checkFavoriteStatus()
                        checkDownloadStatus()
                        if (isLyricsVisible) fetchLyrics(currentArtist, currentSongTitle)
                    }
                }
            })
            loadPlaylistAndPlay()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadPlaylistAndPlay() {
        if (isLocalMode) {
            // Chế độ Offline: Sử dụng SQLite (Room) để lấy danh sách bài hát
            lifecycleScope.launch(Dispatchers.IO) {
                val dbLocal = AppDatabase.getDatabase(this@MainActivity)
                val downloadedList = dbLocal.downloadedSongDao().getAllDownloadedSongs()
                withContext(Dispatchers.Main) {
                    playlistSongs.clear()
                    val mediaItems = mutableListOf<MediaItem>()
                    var startIndex = 0
                    var actualIndex = 0
                    
                    downloadedList.forEach { s ->
                        val file = File(s.localPath)
                        if (file.exists()) {
                            playlistSongs.add(LocalSong(s.id, s.title, s.artist, s.localPath, s.coverUrl, s.duration))
                            if (s.id == songId) startIndex = actualIndex
                            
                            val metadata = MediaMetadata.Builder()
                                .setTitle(s.title)
                                .setArtist(s.artist)
                                .setArtworkUri(Uri.parse(s.coverUrl))
                                .build()
                            
                            mediaItems.add(MediaItem.Builder()
                                .setMediaId(s.id)
                                .setUri(Uri.fromFile(file))
                                .setMediaMetadata(metadata)
                                .build())
                            actualIndex++
                        }
                    }
                    
                    if (mediaItems.isNotEmpty()) {
                        updateAdapterAndPlay(mediaItems, startIndex)
                    } else {
                        Toast.makeText(this@MainActivity, "Không tìm thấy file nhạc trên thiết bị!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Chế độ Online: Lấy từ Firebase
            db.collection("songs").get().addOnSuccessListener { snapshot ->
                playlistSongs.clear()
                val mediaItems = mutableListOf<MediaItem>()
                var startIndex = 0
                snapshot.documents.forEachIndexed { index, doc ->
                    val s = LocalSong(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        audioUrl = doc.getString("audioUrl") ?: "",
                        coverUrl = doc.getString("coverUrl") ?: "",
                        duration = doc.getString("duration") ?: "3:00"
                    )
                    playlistSongs.add(s)
                    if (s.id == songId) startIndex = index
                    val metadata = MediaMetadata.Builder()
                        .setTitle(s.title).setArtist(s.artist)
                        .setArtworkUri(if (s.coverUrl.isNotEmpty()) Uri.parse(s.coverUrl) else null).build()
                    mediaItems.add(MediaItem.Builder()
                        .setMediaId(s.id).setUri(s.audioUrl)
                        .setMediaMetadata(metadata).build())
                }
                updateAdapterAndPlay(mediaItems, startIndex)
            }
        }
    }

    private fun updateAdapterAndPlay(items: List<MediaItem>, startIdx: Int) {
        musicAdapter = MusicPlayerAdapter(playlistSongs) { pos -> 
            controller?.seekToDefaultPosition(pos)
            controller?.play() 
        }
        rvPlaylist.layoutManager = LinearLayoutManager(this)
        rvPlaylist.adapter = musicAdapter
        
        controller?.let { c ->
            c.setMediaItems(items, startIdx, C.TIME_UNSET)
            c.prepare()
            c.play()
        }
    }

    private fun removeAccent(s: String): String {
        val temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D')
    }

    private fun fetchLyrics(artist: String, title: String) {
        tvLyrics.text = "Đang kiểm tra kho lời bài hát..."
        db.collection("songs").document(songId).get().addOnSuccessListener { document ->
            val fbLyrics = document.getString("lyric")
            if (!fbLyrics.isNullOrEmpty()) {
                tvLyrics.text = fbLyrics.replace("\\n", "\n")
            } else {
                Thread {
                    try {
                        val cleanArtist = removeAccent(artist.split("ft.")[0].split("x")[0].trim())
                        val cleanTitle = removeAccent(title.split("(")[0].split("-")[0].trim())
                        val safeArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                        val safeTitle = java.net.URLEncoder.encode(cleanTitle, "UTF-8")
                        val url = java.net.URL("https://api.lyrics.ovh/v1/$safeArtist/$safeTitle")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "GET"
                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().readText()
                            val lyrics = org.json.JSONObject(response).optString("lyrics", "")
                            runOnUiThread { tvLyrics.text = if (lyrics.isNotEmpty()) lyrics else "Chưa có lời cho bài hát này." }
                        } else {
                            runOnUiThread { tvLyrics.text = "Không tìm thấy lời bài hát." }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { tvLyrics.text = "Lỗi kết nối mạng." }
                    }
                }.start()
            }
        }
    }

    private fun setupControls() {
        playBtn.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
        btnBack.setOnClickListener { finish() }
        btnHeart.setOnClickListener { toggleFavorite() }
        btnLyrics.setOnClickListener {
            isLyricsVisible = !isLyricsVisible
            btnLyrics.alpha = if (isLyricsVisible) 1.0f else 0.4f
            cardCoverArt.visibility = if (isLyricsVisible) View.GONE else View.VISIBLE
            scrollLyrics.visibility = if (isLyricsVisible) View.VISIBLE else View.GONE
            if (isLyricsVisible) fetchLyrics(currentArtist, currentSongTitle)
        }
        btnDownload.setOnClickListener { downloadSong() }
        findViewById<ImageView>(R.id.btnPrev).setOnClickListener { if (controller?.hasPreviousMediaItem() == true) controller?.seekToPreviousMediaItem() }
        findViewById<ImageView>(R.id.btnNext).setOnClickListener { if (controller?.hasNextMediaItem() == true) controller?.seekToNextMediaItem() }
    }

    private fun downloadSong() {
        if (audioUrl.isEmpty() || isLocalMode || audioUrl.startsWith("/")) {
            Toast.makeText(this, "Bài hát này đã có sẵn trên máy!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val dbLocal = AppDatabase.getDatabase(this@MainActivity)
            val existing = dbLocal.downloadedSongDao().getDownloadedSongById(songId)
            if (existing != null) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Bài hát này đã được tải về rồi!", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Đang bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
                btnDownload.isEnabled = false
                btnDownload.alpha = 0.5f
            }

            try {
                val fileName = "${songId}.mp3"
                val request = DownloadManager.Request(Uri.parse(audioUrl))
                    .setTitle(currentSongTitle)
                    .setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_MUSIC, fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)

                val localPath = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName).absolutePath
                val downloadedSong = DownloadedSong(
                    id = songId, title = currentSongTitle, artist = currentArtist,
                    audioUrl = audioUrl, localPath = localPath, coverUrl = coverArtUrl, duration = currentDuration
                )
                dbLocal.downloadedSongDao().insert(downloadedSong)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Đã thêm vào danh sách tải xuống!", Toast.LENGTH_SHORT).show()
                    checkDownloadStatus()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Lỗi khi tải xuống: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnDownload.isEnabled = true
                    btnDownload.alpha = 0.4f
                }
            }
        }
    }

    private fun checkDownloadStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbLocal = AppDatabase.getDatabase(this@MainActivity)
            val existing = dbLocal.downloadedSongDao().getDownloadedSongById(songId)
            withContext(Dispatchers.Main) {
                if (existing != null || isLocalMode) {
                    btnDownload.setImageResource(android.R.drawable.stat_sys_download_done)
                    btnDownload.setColorFilter(Color.parseColor("#BB86FC"))
                    btnDownload.alpha = 1.0f
                    btnDownload.isEnabled = false
                } else {
                    btnDownload.setImageResource(android.R.drawable.stat_sys_download)
                    btnDownload.clearColorFilter()
                    btnDownload.alpha = 0.4f
                    btnDownload.isEnabled = true
                }
            }
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) controller?.seekTo(p.toLong()) }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacks(progressRunnable) }
            override fun onStopTrackingTouch(sb: SeekBar?) { if (controller?.isPlaying == true) handler.post(progressRunnable) }
        })
    }

    private fun checkFavoriteStatus() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("favorites").document(uid).collection("songs").document(songId).get().addOnSuccessListener { isFavorite = it.exists(); updateHeartIcon() }
    }

    private fun toggleFavorite() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("favorites").document(uid).collection("songs").document(songId)
        if (isFavorite) ref.delete().addOnSuccessListener { isFavorite = false; updateHeartIcon() }
        else ref.set(mapOf("addedAt" to System.currentTimeMillis())).addOnSuccessListener { isFavorite = true; updateHeartIcon() }
    }

    private fun updateHeartIcon() {
        btnHeart.setImageResource(if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        if (isFavorite) btnHeart.setColorFilter(Color.parseColor("#FF4081")) else btnHeart.clearColorFilter()
    }

    private fun updateTimeDisplay(secs: Int) {
        tvCurrentTime.text = String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60)
    }

    private fun animateEntrance() {
        findViewById<View>(R.id.playerContent).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        controllerFuture?.let { androidx.media3.session.MediaController.releaseFuture(it) }
    }
}

class MusicPlayerAdapter(private val songs: List<LocalSong>, private val onClick: (Int) -> Unit) : RecyclerView.Adapter<MusicPlayerAdapter.VH>() {
    private var currentPlayingPos = 0
    fun updateCurrentPos(pos: Int) { val old = currentPlayingPos; currentPlayingPos = pos; notifyItemChanged(old); notifyItemChanged(pos) }
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = v.findViewById(R.id.tvArtist)
        val imgThumb: ImageView = v.findViewById(R.id.imgThumb)
    }
    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_row, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.tvTitle.text = s.title; holder.tvArtist.text = s.artist
        Glide.with(holder.itemView.context).load(s.coverUrl).placeholder(R.drawable.bg_glow_circle).into(holder.imgThumb)
        holder.tvTitle.setTextColor(if (position == currentPlayingPos) Color.parseColor("#BB86FC") else Color.WHITE)
        holder.itemView.setOnClickListener { onClick(position) }
    }
    override fun getItemCount() = songs.size
}