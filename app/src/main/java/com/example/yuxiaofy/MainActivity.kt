package com.example.yuxiaofy

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import database.ListenHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

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

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val handler = Handler(Looper.getMainLooper())
    private var songId = ""
    private var audioUrl = ""
    private var coverArtUrl = ""

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Bổ sung danh sách playlist
    private val playlistSongs = mutableListOf<SongHome>()
    private lateinit var playlistAdapter: PlaylistAdapter

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
    }

    private fun setupSongInfo() {
        // Load tạm thông tin từ trang trước để UI không bị trống
        songId = intent.getStringExtra("SONG_ID") ?: ""
        val title = intent.getStringExtra("SONG_TITLE") ?: "Unity"
        val artist = intent.getStringExtra("SONG_ARTIST") ?: "TheFatRat"
        audioUrl = intent.getStringExtra("SONG_AUDIO_URL") ?: ""
        coverArtUrl = intent.getStringExtra("SONG_COVER_ART_URL") ?: ""
        val duration = intent.getStringExtra("SONG_DURATION") ?: "3:48"

        tvSongTitle.text = title
        tvArtistName.text = artist
        tvTotalTime.text = duration
        tvCurrentTime.text = "0:00"

        if (coverArtUrl.isNotEmpty()) {
            Glide.with(this).load(coverArtUrl).placeholder(R.drawable.bg_glow_circle).into(coverArt)
        }
        checkFavoriteStatus()
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

                // Chạy khi bài hát bị thay đổi (bấm Next/Prev hoặc tự động qua bài)
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.let {
                        songId = it.mediaId
                        tvSongTitle.text = it.mediaMetadata.title
                        tvArtistName.text = it.mediaMetadata.artist

                        val cover = it.mediaMetadata.artworkUri?.toString() ?: ""
                        if (cover.isNotEmpty()) {
                            Glide.with(this@MainActivity).load(cover).placeholder(R.drawable.bg_glow_circle).into(coverArt)
                        }

                        // Cập nhật dòng sáng màu tím ở list Up Next
                        val currentIndex = controller?.currentMediaItemIndex ?: 0
                        if (::playlistAdapter.isInitialized) {
                            playlistAdapter.updateCurrentPos(currentIndex)
                            rvPlaylist.scrollToPosition(currentIndex)
                        }

                        checkFavoriteStatus()

                        // Lưu bài đang phát vào SharedPreferences cho Mini Player
                        getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit()
                            .putString("now_playing_title", it.mediaMetadata.title?.toString() ?: "")
                            .putString("now_playing_artist", it.mediaMetadata.artist?.toString() ?: "")
                            .apply()

                        // Lưu lịch sử nghe
                        val coverUrl = it.mediaMetadata.artworkUri?.toString() ?: ""
                        val audioUrl = it.localConfiguration?.uri?.toString() ?: ""
                        saveToHistory(it.mediaId, it.mediaMetadata.title?.toString() ?: "", it.mediaMetadata.artist?.toString() ?: "", coverUrl, audioUrl)
                    }
                }
            })

            // Gọi hàm tải playlist
            loadPlaylistAndPlay()

        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadPlaylistAndPlay() {
        db.collection("songs").get().addOnSuccessListener { snapshot ->
            playlistSongs.clear()
            val mediaItems = mutableListOf<MediaItem>()
            var startIndex = 0

            for ((index, doc) in snapshot.documents.withIndex()) {
                val s = SongHome(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    duration = doc.getString("duration") ?: "",
                    audioUrl = doc.getString("audioUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: ""
                )
                playlistSongs.add(s)

                // Tìm vị trí của bài hát mà người dùng vừa click vào
                if (s.id == songId) {
                    startIndex = index
                }

                // Gắn metadata để lúc chuyển bài nó tự đổi Title, Artist, Cover
                val metadata = MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .setArtworkUri(if (s.coverUrl.isNotEmpty()) Uri.parse(s.coverUrl) else null)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(s.id)
                    .setUri(s.audioUrl)
                    .setMediaMetadata(metadata)
                    .build()
                mediaItems.add(mediaItem)
            }

            // Cài đặt RecyclerView danh sách Up Next
            playlistAdapter = PlaylistAdapter(playlistSongs) { position ->
                controller?.seekToDefaultPosition(position)
                controller?.play()
            }
            rvPlaylist.layoutManager = LinearLayoutManager(this)
            rvPlaylist.adapter = playlistAdapter

            // Truyền cả mảng bài hát vào Media3 và phát ở vị trí startIndex
            controller?.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller?.prepare()
            controller?.play()
        }
    }

    private fun setupControls() {
        playBtn.setOnClickListener {
            controller?.let { c ->
                if (c.isPlaying) c.pause() else c.play()
            }
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_up, R.anim.slide_down_fade)
        }

        btnHeart.setOnClickListener {
            toggleFavorite()
            btnHeart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.heart_bounce))
        }

        // CHUYỂN BÀI TRƯỚC
        findViewById<ImageView>(R.id.btnPrev).setOnClickListener {
            if (controller?.hasPreviousMediaItem() == true) {
                controller?.seekToPreviousMediaItem()
            } else {
                controller?.seekTo(0)
            }
        }

        // CHUYỂN BÀI TIẾP THEO
        findViewById<ImageView>(R.id.btnNext).setOnClickListener {
            if (controller?.hasNextMediaItem() == true) {
                controller?.seekToNextMediaItem()
            }
        }

        val btnShuffle = findViewById<ImageView>(R.id.btnShuffle)
        val btnRepeat = findViewById<ImageView>(R.id.btnRepeat)
        var shuffleOn = false
        var repeatOn = false

        btnShuffle.setOnClickListener {
            shuffleOn = !shuffleOn
            btnShuffle.alpha = if (shuffleOn) 1.0f else 0.4f
            controller?.shuffleModeEnabled = shuffleOn
        }
        btnRepeat.setOnClickListener {
            repeatOn = !repeatOn
            btnRepeat.alpha = if (repeatOn) 1.0f else 0.4f
            controller?.repeatMode = if (repeatOn) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    controller?.seekTo(p.toLong())
                    updateTimeDisplay(p / 1000)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
                handler.removeCallbacks(progressRunnable)
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (controller?.isPlaying == true) handler.post(progressRunnable)
            }
        })
    }

    private fun checkFavoriteStatus() {
        val userId = auth.currentUser?.uid ?: return
        if (songId.isEmpty()) return
        db.collection("favorites").document(userId).collection("songs").document(songId).get()
            .addOnSuccessListener { doc -> isFavorite = doc.exists(); updateHeartIcon() }
    }

    private fun toggleFavorite() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }
        if (songId.isEmpty()) return
        val favRef = db.collection("favorites").document(userId).collection("songs").document(songId)
        if (isFavorite) {
            favRef.delete().addOnSuccessListener {
                isFavorite = false; updateHeartIcon()
                Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()
            }
        } else {
            favRef.set(mapOf("addedAt" to System.currentTimeMillis())).addOnSuccessListener {
                isFavorite = true; updateHeartIcon()
                Toast.makeText(this, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateHeartIcon() {
        if (isFavorite) {
            btnHeart.setImageResource(android.R.drawable.btn_star_big_on)
            btnHeart.setColorFilter("#FF4081".toColorInt())
        } else {
            btnHeart.setImageResource(android.R.drawable.btn_star_big_off)
            btnHeart.clearColorFilter()
        }
    }

    private fun updateTimeDisplay(secs: Int) {
        tvCurrentTime.text = String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60)
    }

    private fun animateEntrance() {
        findViewById<View>(R.id.playerContent).startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        )
    }

    private fun saveToHistory(id: String, title: String, artist: String, coverUrl: String, audioUrl: String) {
        if (id.isEmpty() || title.isEmpty()) return
        val userId = auth.currentUser?.uid ?: return  // Không lưu nếu chưa đăng nhập
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.listenHistoryDao().addToHistory(
                ListenHistory(userId = userId, songId = id, title = title, artist = artist, coverUrl = coverUrl, audioUrl = audioUrl)
            )
            // Xóa lịch sử cũ hơn 30 ngày của user này
            db.listenHistoryDao().deleteOldHistoryByUser(userId, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        if (rotateAnimator.isRunning) rotateAnimator.cancel()
    }
}

// Bổ sung callback onClick để bấm vào bài nào phát bài đó
class PlaylistAdapter(
    private val songs: List<SongHome>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

    private var currentPlayingPos = 0

    fun updateCurrentPos(pos: Int) {
        val old = currentPlayingPos
        currentPlayingPos = pos
        notifyItemChanged(old)
        notifyItemChanged(pos)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvIndex: TextView = v.findViewById(R.id.tvIndex)
        val tvTitle: TextView = v.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = v.findViewById(R.id.tvArtist)
        val tvDuration: TextView = v.findViewById(R.id.tvDuration)
        val imgThumb: ImageView = v.findViewById(R.id.imgThumb)
        val nowPlayingBar: View = v.findViewById(R.id.nowPlayingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_row, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[holder.bindingAdapterPosition]
        holder.tvTitle.text = s.title
        holder.tvArtist.text = s.artist
        holder.tvDuration.text = s.duration

        // Load ảnh thật cho các bài trong danh sách Up Next
        if (s.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(s.coverUrl).into(holder.imgThumb)
        } else {
            holder.imgThumb.setImageResource(R.drawable.ic_launcher_background)
        }

        if (holder.bindingAdapterPosition == currentPlayingPos) {
            holder.tvTitle.setTextColor("#BB86FC".toColorInt())
            holder.tvIndex.setTextColor("#BB86FC".toColorInt())
            holder.nowPlayingBar.visibility = View.VISIBLE
            holder.tvIndex.text = "▶"
        } else {
            holder.tvTitle.setTextColor(Color.WHITE)
            holder.tvIndex.setTextColor("#888888".toColorInt())
            holder.nowPlayingBar.visibility = View.GONE
            holder.tvIndex.text = (holder.bindingAdapterPosition + 1).toString()
        }

        // Bấm bài nào, chạy bài đó
        holder.itemView.setOnClickListener {
            onClick(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = songs.size
}