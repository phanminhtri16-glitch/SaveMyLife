package com.example.yuxiaofy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
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

class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
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

    private val allSongsFromDB = mutableListOf<SongHome>()
    private var currentCategory = "Chill"
    private var allSongs = mutableListOf<SongHome>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        db = FirebaseFirestore.getInstance()
        bindViews()
        setupUserGreeting()
        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupMiniPlayer()
        setupBottomNav()
        animateEntrance()
        loadSongsFromFirestore()
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
    }

    private fun loadSongsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        db.collection("songs").addSnapshotListener { snapshot, error ->
            progressBar.visibility = View.GONE
            if (error != null || snapshot == null) return@addSnapshotListener
            allSongsFromDB.clear()
            snapshot.documents.forEach { doc ->
                allSongsFromDB.add(SongHome(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    imageRes = R.drawable.ic_launcher_background,
                    duration = doc.getString("duration") ?: "3:00",
                    audioUrl = doc.getString("audioUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    category = doc.getString("category") ?: ""
                ))
            }
            filterByCategory(currentCategory)
        }
    }

    private fun filterByCategory(category: String) {
        currentCategory = category
        allSongs = if (category == "Tất cả") allSongsFromDB.toMutableList()
        else allSongsFromDB.filter { it.category.equals(category, ignoreCase = true) }.toMutableList()
        val query = etSearch.text.toString().lowercase(Locale.getDefault())
        songAdapter.updateData(if (query.isEmpty()) allSongs
        else allSongs.filter { it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) })
        rvSongs.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up))
    }

    private fun setupUserGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) { in 5..11 -> "Good Morning,"; in 12..17 -> "Good Afternoon,"; else -> "Good Evening," }
        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        tvUserName.text = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
    }

    private fun setupRecyclerView() {
        rvSongs.layoutManager = LinearLayoutManager(this)
        rvSongs.isNestedScrollingEnabled = false
        songAdapter = HomeSongAdapter(mutableListOf()) { song ->
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_ID", song.id)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
                putExtra("SONG_AUDIO_URL", song.audioUrl)
                putExtra("SONG_DURATION", song.duration)
            })
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvSongs.adapter = songAdapter
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
                songAdapter.updateData(if (query.isEmpty()) allSongs
                else allSongs.filter { it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) })
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupMiniPlayer() {
        tvMiniTitle.text = "Unity"; tvMiniArtist.text = "TheFatRat"
        miniPlayer.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        btnMiniPlay.setOnClickListener { btnMiniPlay.setImageResource(android.R.drawable.ic_media_pause) }
    }

    private fun setupBottomNav() {
        btnNavHome.setOnClickListener { }
        btnNavSearch.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)); overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) }
        btnNavFavorites.setOnClickListener { startActivity(Intent(this, FavoritesActivity::class.java)); overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) }
        btnNavProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)); overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) }
    }

    private fun animateEntrance() {
        findViewById<View>(R.id.headerSection).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up))
    }
}

class HomeSongAdapter(
    private var songs: MutableList<SongHome>,
    private val onClick: (SongHome) -> Unit
) : RecyclerView.Adapter<HomeSongAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvHomeSongTitle)
        val artist: TextView = v.findViewById(R.id.tvHomeArtist)
        val duration: TextView = v.findViewById(R.id.tvDuration)
        val img: ImageView = v.findViewById(R.id.imgSongCover)
        val heart: ImageView = v.findViewById(R.id.btnHeart)
        val container: View = v.findViewById(R.id.songItemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_home_song, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.title.text = s.title; holder.artist.text = s.artist; holder.duration.text = s.duration
        holder.img.setImageResource(R.drawable.ic_launcher_background)
        updateHeartIcon(holder.heart, s.isFavorite)
        holder.heart.setOnClickListener {
            s.isFavorite = !s.isFavorite; updateHeartIcon(holder.heart, s.isFavorite)
            holder.heart.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.heart_bounce))
        }
        holder.itemView.setOnClickListener { onClick(s) }
    }

    private fun updateHeartIcon(iv: ImageView, isFav: Boolean) {
        if (isFav) { iv.setImageResource(android.R.drawable.btn_star_big_on); iv.setColorFilter(Color.parseColor("#FF4081")) }
        else { iv.setImageResource(android.R.drawable.btn_star_big_off); iv.clearColorFilter() }
    }

    override fun getItemCount() = songs.size
    fun updateData(newSongs: List<SongHome>) { songs = newSongs.toMutableList(); notifyDataSetChanged() }
}