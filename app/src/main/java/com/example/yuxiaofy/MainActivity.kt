package com.example.yuxiaofy

import android.animation.ObjectAnimator
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rotateAnimator: ObjectAnimator
    private var isPlaying = false
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var playBtn: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvArtistName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnHeart: ImageView
    private var isFavorite = false

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var songId = ""
    private var audioUrl = ""

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    seekBar.progress = mp.currentPosition
                    updateTimeDisplay(mp.currentPosition / 1000)
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
        checkFavoriteStatus()

        if (audioUrl.isNotEmpty()) initMediaPlayer(audioUrl)
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
    }

    private fun setupSongInfo() {
        songId = intent.getStringExtra("SONG_ID") ?: ""
        val title = intent.getStringExtra("SONG_TITLE") ?: "Unity"
        val artist = intent.getStringExtra("SONG_ARTIST") ?: "TheFatRat"
        audioUrl = intent.getStringExtra("SONG_AUDIO_URL") ?: ""
        val duration = intent.getStringExtra("SONG_DURATION") ?: "3:48"
        tvSongTitle.text = title
        tvArtistName.text = artist
        tvTotalTime.text = duration
        tvCurrentTime.text = "0:00"
    }

    private fun initMediaPlayer(url: String) {
        mediaPlayer?.release()
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        try {
            mp.setDataSource(url)
            mp.setOnPreparedListener { player ->
                seekBar.max = player.duration
                val totalSecs = player.duration / 1000
                tvTotalTime.text = String.format(Locale.getDefault(), "%d:%02d", totalSecs / 60, totalSecs % 60)
                player.start()
                isPlaying = true
                playBtn.setImageResource(android.R.drawable.ic_media_pause)
                if (rotateAnimator.isPaused) rotateAnimator.resume() else rotateAnimator.start()
                handler.post(progressRunnable)
            }
            mp.setOnCompletionListener {
                isPlaying = false
                playBtn.setImageResource(android.R.drawable.ic_media_play)
                rotateAnimator.pause()
                handler.removeCallbacks(progressRunnable)
            }
            mp.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Không thể phát bài hát này!", Toast.LENGTH_SHORT).show()
                true
            }
            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCoverArtAnimation() {
        val coverArt = findViewById<ImageView>(R.id.cover_art)
        rotateAnimator = ObjectAnimator.ofFloat(coverArt, "rotation", 0f, 360f).apply {
            duration = 12000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun setupControls() {
        playBtn.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_up, R.anim.slide_down_fade)
        }

        btnHeart.setOnClickListener {
            toggleFavorite()
            btnHeart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.heart_bounce))
        }

        findViewById<ImageView>(R.id.btnPrev).setOnClickListener {
            mediaPlayer?.seekTo(0); seekBar.progress = 0; updateTimeDisplay(0)
        }

        findViewById<ImageView>(R.id.btnNext).setOnClickListener {
            mediaPlayer?.seekTo(0); seekBar.progress = 0; updateTimeDisplay(0)
        }

        val btnShuffle = findViewById<ImageView>(R.id.btnShuffle)
        val btnRepeat = findViewById<ImageView>(R.id.btnRepeat)
        var shuffleOn = false
        var repeatOn = false

        btnShuffle.setOnClickListener {
            shuffleOn = !shuffleOn
            btnShuffle.alpha = if (shuffleOn) 1.0f else 0.4f
        }
        btnRepeat.setOnClickListener {
            repeatOn = !repeatOn
            btnRepeat.alpha = if (repeatOn) 1.0f else 0.4f
            mediaPlayer?.isLooping = repeatOn
        }
    }

    private fun playMusic() {
        mediaPlayer?.let { mp ->
            mp.start(); isPlaying = true
            playBtn.setImageResource(android.R.drawable.ic_media_pause)
            if (rotateAnimator.isPaused) rotateAnimator.resume() else rotateAnimator.start()
            handler.post(progressRunnable)
        } ?: run {
            if (audioUrl.isNotEmpty()) initMediaPlayer(audioUrl)
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        playBtn.setImageResource(android.R.drawable.ic_media_play)
        rotateAnimator.pause()
        handler.removeCallbacks(progressRunnable)
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { mediaPlayer?.seekTo(p); updateTimeDisplay(p / 1000) }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacks(progressRunnable) }
            override fun onStopTrackingTouch(sb: SeekBar?) { if (isPlaying) handler.post(progressRunnable) }
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

    override fun onPause() {
        super.onPause()
        if (isPlaying) pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        if (rotateAnimator.isRunning) rotateAnimator.cancel()
    }
}

class PlaylistAdapter(private val songs: List<SongHome>) :
    RecyclerView.Adapter<PlaylistAdapter.VH>() {

    private var currentPlayingPos = 0

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
        holder.imgThumb.setImageResource(R.drawable.ic_launcher_background)
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
        holder.itemView.setOnClickListener {
            val old = currentPlayingPos
            currentPlayingPos = holder.bindingAdapterPosition
            notifyItemChanged(old)
            notifyItemChanged(currentPlayingPos)
        }
    }

    override fun getItemCount() = songs.size
}