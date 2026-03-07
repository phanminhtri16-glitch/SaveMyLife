package com.example.yuxiaofy

import android.animation.ObjectAnimator
import android.graphics.Color
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

    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0
    private val totalDuration = 228 // 3:48 in seconds

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && progress < totalDuration) {
                progress++
                seekBar.progress = progress
                updateTimeDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        setupPlaylist()
        animateEntrance()
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
        val title = intent.getStringExtra("SONG_TITLE") ?: "Unity"
        val artist = intent.getStringExtra("SONG_ARTIST") ?: "TheFatRat"
        tvSongTitle.text = title
        tvArtistName.text = artist
        tvTotalTime.text = "3:48"
        tvCurrentTime.text = "0:00"
        seekBar.max = totalDuration
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
        playBtn.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_up, R.anim.slide_down_fade)
        }

        btnHeart.setOnClickListener {
            isFavorite = !isFavorite
            btnHeart.setImageResource(
                if (isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            if (isFavorite) btnHeart.setColorFilter("#FF4081".toColorInt())
            else btnHeart.clearColorFilter()
            val bounce = AnimationUtils.loadAnimation(this, R.anim.heart_bounce)
            btnHeart.startAnimation(bounce)
        }

        findViewById<ImageView>(R.id.btnPrev).setOnClickListener {
            progress = 0
            seekBar.progress = 0
            updateTimeDisplay()
        }

        findViewById<ImageView>(R.id.btnNext).setOnClickListener {
            progress = 0
            seekBar.progress = 0
            updateTimeDisplay()
            if (isPlaying) {
                handler.removeCallbacks(progressRunnable)
                handler.postDelayed(progressRunnable, 1000)
            }
        }

        // Shuffle & Repeat buttons
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
        }
    }

    private fun playMusic() {
        isPlaying = true
        playBtn.setImageResource(android.R.drawable.ic_media_pause)
        if (rotateAnimator.isPaused) rotateAnimator.resume() else rotateAnimator.start()
        handler.postDelayed(progressRunnable, 1000)
    }

    private fun pauseMusic() {
        isPlaying = false
        playBtn.setImageResource(android.R.drawable.ic_media_play)
        rotateAnimator.pause()
        handler.removeCallbacks(progressRunnable)
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    progress = p; updateTimeDisplay()
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                handler.removeCallbacks(progressRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (isPlaying) handler.postDelayed(progressRunnable, 1000)
            }
        })
    }

    private fun setupPlaylist() {
        val rv = findViewById<RecyclerView>(R.id.rvPlaylist)
        rv.layoutManager = LinearLayoutManager(this)
        val songs = listOf(
            SongHome("Unity", "TheFatRat", R.drawable.ic_launcher_background, true, "3:48"),
            SongHome(
                "Monody",
                "TheFatRat ft. Laura Brehm",
                R.drawable.ic_launcher_background,
                false,
                "4:12"
            ),
            SongHome(
                "The Calling",
                "TheFatRat ft. Laura Brehm",
                R.drawable.ic_launcher_background,
                false,
                "4:07"
            ),
            SongHome("Xenogenesis", "TheFatRat", R.drawable.ic_launcher_background, false, "5:01"),
            SongHome(
                "Fly Away",
                "TheFatRat ft. Anjulie",
                R.drawable.ic_launcher_background,
                false,
                "3:55"
            ),
            SongHome("Time Lapse", "TheFatRat", R.drawable.ic_launcher_background, false, "3:21")
        )
        rv.adapter = PlaylistAdapter(songs)
    }

    private fun animateEntrance() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.playerContent).startAnimation(slideUp)
    }

    private fun updateTimeDisplay() {
        val mins = progress / 60
        val secs = progress % 60
        tvCurrentTime.text = String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
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

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[holder.bindingAdapterPosition]
        holder.tvTitle.text = s.title
        holder.tvArtist.text = s.artist
        holder.tvDuration.text = s.duration
        holder.imgThumb.setImageResource(s.imageRes)

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
