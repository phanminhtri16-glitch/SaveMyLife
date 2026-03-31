package com.example.yuxiaofy

import android.app.DownloadManager
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
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
import java.util.Calendar
import java.util.Locale

data class SongHome(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val imageRes: Int = 0,
    var isFavorite: Boolean = false,
    val duration: String = "3:30",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val category: String = ""
)

@androidx.media3.common.util.UnstableApi
class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null
    private lateinit var imgMiniCover: ImageView
    private lateinit var rvSongs: RecyclerView
    private lateinit var songAdapter: HomeSongAdapter
    private lateinit var listTitle: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var miniPlayer: CardView
    private lateinit var etSearch: EditText
    private lateinit var btnChill: TextView
    private lateinit var btnWorkout: TextView
    private lateinit var btnFocus: TextView
    private lateinit var btnRB: TextView
    private lateinit var tvMiniTitle: TextView
    private lateinit var tvMiniArtist: TextView
    private lateinit var btnMiniPlay: ImageView
    private lateinit var btnNavHome: LinearLayout
    private lateinit var btnNavSearch: LinearLayout
    private lateinit var btnNavFavorites: LinearLayout
    private lateinit var btnNavProfile: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMiniPrev: ImageView
    private lateinit var btnMiniNext: ImageView
    private lateinit var miniProgressBar: ProgressBar
    private lateinit var btnAdmin: ImageView

    private val allSongsFromDB = mutableListOf<SongHome>()
    private var currentCategory = "Chill"
    private var allSongs = mutableListOf<SongHome>()

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            controller?.let { c ->
                if (c.isPlaying) {
                    miniProgressBar.max = c.duration.toInt().coerceAtLeast(0)
                    miniProgressBar.progress = c.currentPosition.toInt()
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        bindViews()
        setupUserGreeting()
        setupAdminButton()
        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupMiniPlayer()
        setupBottomNav()
        animateEntrance()
        loadSongsFromFirestore()
        setupMediaController()
    }

    private fun bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvUserName = findViewById(R.id.tvUserName)
        listTitle = findViewById(R.id.tvListTitle)
        miniPlayer = findViewById(R.id.miniPlayer)
        rvSongs = findViewById(R.id.rvSongs)
        etSearch = findViewById(R.id.etSearch)
        btnChill = findViewById(R.id.btnCatChill)
        btnWorkout = findViewById(R.id.btnCatWorkout)
        btnFocus = findViewById(R.id.btnCatFocus)
        btnRB = findViewById(R.id.btnCatRB)
        tvMiniTitle = findViewById(R.id.tvMiniTitle)
        tvMiniArtist = findViewById(R.id.tvMiniArtist)
        btnMiniPlay = findViewById(R.id.btnMiniPlay)
        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavSearch = findViewById(R.id.btnNavSearch)
        btnNavFavorites = findViewById(R.id.btnNavFavorites)
        btnNavProfile = findViewById(R.id.btnNavProfile)
        progressBar = findViewById(R.id.homeProgressBar)
        imgMiniCover = findViewById(R.id.imgMiniCover)
        btnMiniPrev = findViewById(R.id.btnMiniPrev)
        btnMiniNext = findViewById(R.id.btnMiniNext)
        miniProgressBar = findViewById(R.id.miniProgressBar)
        btnAdmin = findViewById(R.id.btnAdmin)
    }

    private fun setupAdminButton() {
        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("is_admin", false)
        if (isAdmin) {
            btnAdmin.visibility = View.VISIBLE
            btnAdmin.setOnClickListener {
                startActivity(Intent(this, AdminActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        } else {
            btnAdmin.visibility = View.GONE
        }
    }

    private fun loadSongsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        val uid = auth.currentUser?.uid
        db.collection("songs").addSnapshotListener { snapshot, error ->
            progressBar.visibility = View.GONE
            if (error != null || snapshot == null) return@addSnapshotListener
            allSongsFromDB.clear()
            snapshot.documents.forEach { doc ->
                allSongsFromDB.add(
                    SongHome(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        imageRes = R.drawable.ic_launcher_background,
                        duration = doc.getString("duration") ?: "3:00",
                        audioUrl = doc.getString("audioUrl") ?: "",
                        coverUrl = doc.getString("coverUrl") ?: "",
                        category = doc.getString("category") ?: ""
                    )
                )
            }
            setupFeaturedBanner()
            if (uid != null) {
                db.collection("favorites").document(uid).collection("songs").get()
                    .addOnSuccessListener { favSnap ->
                        val favIds = favSnap.documents.map { it.id }.toSet()
                        allSongsFromDB.forEach { it.isFavorite = it.id in favIds }
                        filterByCategory(currentCategory)
                    }
                    .addOnFailureListener { filterByCategory(currentCategory) }
            } else {
                filterByCategory(currentCategory)
            }
        }
    }

    private fun setupFeaturedBanner() {
        if (allSongsFromDB.isEmpty()) return

        val uid = auth.currentUser?.uid ?: return fallbackFeaturedBanner()

        lifecycleScope.launch(Dispatchers.IO) {
            val dbLocal = database.AppDatabase.getDatabase(this@HomeActivity)
            val history = dbLocal.listenHistoryDao().getHistoryByUser(uid)
            val recent10 = history.take(10)

            // Map history songs to their category by searching in allSongsFromDB
            val categories = recent10.mapNotNull { h ->
                allSongsFromDB.find { it.id == h.songId }?.category
            }

            val mostFrequentCategory = categories.groupBy { it }.maxByOrNull { it.value.size }?.key

            val recommendedSong = if (mostFrequentCategory != null) {
                val songsInCategory = allSongsFromDB.filter { it.category == mostFrequentCategory }
                if (songsInCategory.isNotEmpty()) songsInCategory.random() else allSongsFromDB.random()
            } else {
                allSongsFromDB.random()
            }

            withContext(Dispatchers.Main) {
                displayBannerSong(recommendedSong)
            }
        }
    }

    private fun fallbackFeaturedBanner() {
        if (allSongsFromDB.isEmpty()) return
        displayBannerSong(allSongsFromDB.random())
    }

    private fun displayBannerSong(song: SongHome) {
        val tvFeaturedTitle = findViewById<TextView>(R.id.tvFeaturedTitle)
        val imgFeaturedBanner = findViewById<ImageView>(R.id.imgFeaturedBanner)
        val featuredCard = findViewById<androidx.cardview.widget.CardView>(R.id.featuredCard)

        tvFeaturedTitle.text = "${song.title} · ${song.artist}"
        if (song.coverUrl.isNotEmpty()) {
            Glide.with(this).load(song.coverUrl).centerCrop()
                .placeholder(R.drawable.ic_launcher_background).into(imgFeaturedBanner)
        }
        featuredCard.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_ID", song.id)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
                putExtra("SONG_AUDIO_URL", song.audioUrl)
                putExtra("SONG_COVER_URL", song.coverUrl)
                putExtra("SONG_DURATION", song.duration)
            })
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
    }

    private fun filterByCategory(category: String) {
        currentCategory = category
        allSongs = if (category == "Tất cả") allSongsFromDB.toMutableList()
        else allSongsFromDB.filter { it.category.equals(category, ignoreCase = true) }
            .toMutableList()
        val query = etSearch.text.toString().lowercase(Locale.getDefault())
        songAdapter.updateData(
            if (query.isEmpty()) allSongs
            else allSongs.filter {
                it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
            })
    }

    private fun setupUserGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        tvUserName.text = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
    }

    private fun setupRecyclerView() {
        rvSongs.layoutManager = LinearLayoutManager(this)
        rvSongs.isNestedScrollingEnabled = false
        songAdapter = HomeSongAdapter(
            mutableListOf(),
            onItemClick = { song ->
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("SONG_ID", song.id)
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_AUDIO_URL", song.audioUrl)
                    putExtra("SONG_COVER_URL", song.coverUrl)
                    putExtra("SONG_DURATION", song.duration)
                })
                overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
            },
            onDownloadClick = { song ->
                downloadSong(song)
            },
            onFavoriteClick = { song, isFav ->
                val uid = auth.currentUser?.uid ?: return@HomeSongAdapter
                val ref =
                    db.collection("favorites").document(uid).collection("songs").document(song.id)
                if (isFav) ref.set(mapOf("addedAt" to System.currentTimeMillis()))
                else ref.delete()
            }
        )
        rvSongs.adapter = songAdapter
    }

    private fun downloadSong(song: SongHome) {
        if (song.audioUrl.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đường dẫn tải!", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val dbLocal = AppDatabase.getDatabase(this@HomeActivity)
            val existing = dbLocal.downloadedSongDao().getDownloadedSongById(song.id)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Đã tải về rồi!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@HomeActivity,
                    "Bắt đầu tải: ${song.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            try {
                val fileName = "${song.id}.mp3"
                val request = DownloadManager.Request(Uri.parse(song.audioUrl))
                    .setTitle(song.title)
                    .setDestinationInExternalFilesDir(
                        this@HomeActivity,
                        Environment.DIRECTORY_MUSIC,
                        fileName
                    )
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                val localPath =
                    File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName).absolutePath
                val downloadedSong = DownloadedSong(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    audioUrl = song.audioUrl,
                    localPath = localPath,
                    coverUrl = song.coverUrl,
                    duration = song.duration
                )
                dbLocal.downloadedSongDao().insert(downloadedSong)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Đã thêm vào danh sách tải xuống!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Lỗi tải: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupCategoryChips() {
        setActiveChip(btnChill)
        btnChill.setOnClickListener { switchCategory(btnChill, "Chill Vibes", "Chill") }
        btnWorkout.setOnClickListener { switchCategory(btnWorkout, "Workout Energy", "Workout") }
        btnFocus.setOnClickListener { switchCategory(btnFocus, "Deep Focus", "Focus") }
        btnRB.setOnClickListener { switchCategory(btnRB, "R&B Classics", "RB") }
    }

    private fun switchCategory(btn: TextView, title: String, tag: String) {
        if (currentCategory == tag) return
        listOf(btnChill, btnWorkout, btnFocus, btnRB).forEach { resetChip(it) }
        setActiveChip(btn); listTitle.text = title; filterByCategory(tag)
        rvSongs.smoothScrollToPosition(0)
    }

    private fun setActiveChip(btn: TextView) {
        btn.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_active)
        btn.setTextColor(Color.BLACK)
    }

    private fun resetChip(btn: TextView) {
        btn.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_inactive)
        btn.setTextColor(Color.WHITE)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase(Locale.getDefault())
                songAdapter.updateData(
                    if (query.isEmpty()) allSongs
                    else allSongs.filter {
                        it.title.lowercase().contains(query) || it.artist.lowercase()
                            .contains(query)
                    })
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupMiniPlayer() {
        miniPlayer.setOnClickListener {
            val c = controller
            if (c != null && c.currentMediaItem != null) {
                val meta = c.mediaMetadata
                val songId = c.currentMediaItem?.mediaId ?: ""
                // Tìm audioUrl từ allSongsFromDB theo id
                val song = allSongsFromDB.find { it.id == songId }
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("SONG_ID", songId)
                    putExtra("SONG_TITLE", meta.title?.toString() ?: "")
                    putExtra("SONG_ARTIST", meta.artist?.toString() ?: "")
                    putExtra("SONG_AUDIO_URL", song?.audioUrl ?: "")
                    putExtra("SONG_COVER_URL", meta.artworkUri?.toString() ?: song?.coverUrl ?: "")
                    putExtra("SONG_DURATION", song?.duration ?: "")
                })
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        btnMiniPlay.setOnClickListener {
            controller?.let { c -> if (c.isPlaying) c.pause() else c.play() }
        }
        btnMiniPrev.setOnClickListener {
            controller?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }
        }
        btnMiniNext.setOnClickListener {
            controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
        }
        updateMiniPlayerUI()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    btnMiniPlay.setImageResource(
                        if (isPlaying) R.drawable.playbutton
                        else R.drawable.pausebutton
                    )
                    if (isPlaying) {
                        handler.post(progressRunnable)
                    } else {
                        handler.removeCallbacks(progressRunnable)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateMiniPlayerUI()
                }
            })
            updateMiniPlayerUI()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateMiniPlayerUI() {
        val c = controller
        if (c != null && c.currentMediaItem != null) {
            val meta = c.mediaMetadata
            val title = meta.title?.toString() ?: ""
            val artist = meta.artist?.toString() ?: ""
            val artworkUri = meta.artworkUri

            if (title.isNotEmpty()) {
                tvMiniTitle.text = title
                tvMiniArtist.text = artist
                miniPlayer.visibility = View.VISIBLE
            }
            if (artworkUri != null) {
                Glide.with(this).load(artworkUri).placeholder(R.drawable.ic_launcher_background)
                    .centerCrop().into(imgMiniCover)
            }
            btnMiniPlay.setImageResource(
                if (c.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
            miniProgressBar.max = c.duration.toInt().coerceAtLeast(0)
            miniProgressBar.progress = c.currentPosition.toInt()
            if (c.isPlaying && miniPlayer.visibility == View.VISIBLE) {
                handler.post(progressRunnable)
            }
        } else {
            // Fallback về SharedPreferences nếu chưa có controller
            val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
            val nowTitle = prefs.getString("now_playing_title", "") ?: ""
            val nowArtist = prefs.getString("now_playing_artist", "") ?: ""
            if (nowTitle.isNotEmpty()) {
                tvMiniTitle.text = nowTitle
                tvMiniArtist.text = nowArtist
                miniPlayer.visibility = View.VISIBLE
            } else {
                miniPlayer.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    private fun setupBottomNav() {
        btnNavHome.setOnClickListener { }
        btnNavSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnNavFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnNavProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayerUI()
    }

    private fun animateEntrance() {
        findViewById<View>(R.id.headerSection).startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.fade_in_up
            )
        )
    }
}

class HomeSongAdapter(
    private var songs: MutableList<SongHome>,
    private val onItemClick: (SongHome) -> Unit,
    private val onDownloadClick: (SongHome) -> Unit,
    private val onFavoriteClick: ((SongHome, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<HomeSongAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvHomeSongTitle)
        val artist: TextView = v.findViewById(R.id.tvHomeArtist)
        val duration: TextView = v.findViewById(R.id.tvDuration)
        val img: ImageView = v.findViewById(R.id.imgSongCover)
        val heart: ImageView = v.findViewById(R.id.btnHeart)
        val download: ImageView = v.findViewById(R.id.btnHomeDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_home_song, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.title.text = s.title
        holder.artist.text = s.artist
        holder.duration.text = s.duration

        if (s.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(s.coverUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.img)
        }

        updateHeartIcon(holder.heart, s.isFavorite)
        holder.heart.setOnClickListener {
            s.isFavorite = !s.isFavorite
            updateHeartIcon(holder.heart, s.isFavorite)
            holder.heart.startAnimation(
                AnimationUtils.loadAnimation(
                    holder.itemView.context,
                    R.anim.heart_bounce
                )
            )
            onFavoriteClick?.invoke(s, s.isFavorite)
        }

        holder.download.setOnClickListener {
            onDownloadClick(s)
            holder.download.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                holder.download.animate().scaleX(1.0f).scaleY(1.0f).start()
            }
        }

        holder.itemView.setOnClickListener { onItemClick(s) }
    }

    private fun updateHeartIcon(iv: ImageView, isFav: Boolean) {
        if (isFav) {
            iv.setImageResource(android.R.drawable.btn_star_big_on); iv.setColorFilter(
                Color.parseColor(
                    "#FF4081"
                )
            )
        } else {
            iv.setImageResource(android.R.drawable.btn_star_big_off); iv.clearColorFilter()
        }
    }

    override fun getItemCount() = songs.size
    fun updateData(newSongs: List<SongHome>) {
        songs = newSongs.toMutableList(); notifyDataSetChanged()
    }
}
