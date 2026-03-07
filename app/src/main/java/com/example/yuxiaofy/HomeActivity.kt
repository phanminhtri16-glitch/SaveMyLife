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
import java.util.Calendar
import java.util.Locale

data class SongHome(
    val title: String,
    val artist: String,
    val imageRes: Int,
    var isFavorite: Boolean = false,
    val duration: String = "3:30"
)

class HomeActivity : AppCompatActivity() {

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

    private val chillList = listOf(
        SongHome("Unity", "TheFatRat", R.drawable.ic_launcher_background, true, "3:48"),
        SongHome(
            "Monody",
            "TheFatRat ft. Laura Brehm",
            R.drawable.ic_launcher_background,
            false,
            "4:12"
        ),
        SongHome("Time Lapse", "TheFatRat", R.drawable.ic_launcher_background, false, "3:21"),
        SongHome("Infinite Power", "TheFatRat", R.drawable.ic_launcher_background, true, "3:55"),
        SongHome(
            "The Calling",
            "TheFatRat ft. Laura Brehm",
            R.drawable.ic_launcher_background,
            false,
            "4:07"
        )
    )
    private val workoutList = listOf(
        SongHome("Stronger", "Kanye West", R.drawable.ic_launcher_background, false, "5:11"),
        SongHome("Believer", "Imagine Dragons", R.drawable.ic_launcher_background, false, "3:23"),
        SongHome("Radioactive", "Imagine Dragons", R.drawable.ic_launcher_background, true, "3:06"),
        SongHome("HUMBLE.", "Kendrick Lamar", R.drawable.ic_launcher_background, false, "2:57")
    )
    private val focusList = listOf(
        SongHome("River Flows in You", "Yiruma", R.drawable.ic_launcher_background, true, "3:52"),
        SongHome("Weightless", "Marconi Union", R.drawable.ic_launcher_background, false, "8:09"),
        SongHome(
            "Experience",
            "Ludovico Einaudi",
            R.drawable.ic_launcher_background,
            false,
            "5:16"
        ),
        SongHome(
            "Nuvole Bianche",
            "Ludovico Einaudi",
            R.drawable.ic_launcher_background,
            false,
            "5:57"
        )
    )
    private val rbList = listOf(
        SongHome("Blinding Lights", "The Weeknd", R.drawable.ic_launcher_background, true, "3:20"),
        SongHome(
            "Stay",
            "Kid LAROI & Justin Bieber",
            R.drawable.ic_launcher_background,
            false,
            "2:21"
        ),
        SongHome("Peaches", "Justin Bieber", R.drawable.ic_launcher_background, false, "3:18"),
        SongHome(
            "Leave The Door Open",
            "Silk Sonic",
            R.drawable.ic_launcher_background,
            true,
            "4:02"
        )
    )

    private var currentCategory = "Chill"
    private var allSongs = chillList.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindViews()
        setupUserGreeting()
        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupMiniPlayer()
        setupBottomNav()
        animateEntrance()
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
    }

    private fun setupUserGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
        tvGreeting.text = greeting

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val name = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
        tvUserName.text = name
    }

    private fun setupRecyclerView() {
        rvSongs.layoutManager = LinearLayoutManager(this)
        rvSongs.isNestedScrollingEnabled = false
        songAdapter = HomeSongAdapter(chillList.toMutableList()) { song ->
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvSongs.adapter = songAdapter
    }

    private fun setupCategoryChips() {
        setActiveChip(btnChill)
        btnChill.setOnClickListener { switchCategory(btnChill, "Chill Vibes", chillList, "Chill") }
        btnWorkout.setOnClickListener {
            switchCategory(
                btnWorkout,
                "Workout Energy",
                workoutList,
                "Workout"
            )
        }
        btnFocus.setOnClickListener { switchCategory(btnFocus, "Deep Focus", focusList, "Focus") }
        btnRB.setOnClickListener { switchCategory(btnRB, "R&B Classics", rbList, "RB") }
    }

    private fun switchCategory(btn: TextView, title: String, songs: List<SongHome>, tag: String) {
        if (currentCategory == tag) return
        currentCategory = tag
        listOf(btnChill, btnWorkout, btnFocus, btnRB).forEach { resetChip(it) }
        setActiveChip(btn)
        listTitle.text = title
        allSongs = songs.toMutableList()
        songAdapter.updateData(allSongs)
        rvSongs.smoothScrollToPosition(0)
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        rvSongs.startAnimation(anim)
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
                val filtered = if (query.isEmpty()) allSongs
                else allSongs.filter {
                    it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                }
                songAdapter.updateData(filtered)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupMiniPlayer() {
        tvMiniTitle.text = "Unity"
        tvMiniArtist.text = "TheFatRat"
        miniPlayer.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        btnMiniPlay.setOnClickListener {
            // Toggle play icon
            btnMiniPlay.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun setupBottomNav() {
        btnNavHome.setOnClickListener { /* Already home */ }
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

    private fun animateEntrance() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.headerSection).startAnimation(fadeIn)
    }
}

// ─── ADAPTER ───────────────────────────────────────────────────────────────
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_home_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.title.text = s.title
        holder.artist.text = s.artist
        holder.duration.text = s.duration
        try {
            holder.img.setImageResource(s.imageRes)
        } catch (e: Exception) {
            holder.img.setImageResource(R.drawable.ic_launcher_background)
        }

        updateHeartIcon(holder.heart, s.isFavorite)
        holder.heart.setOnClickListener {
            s.isFavorite = !s.isFavorite
            updateHeartIcon(holder.heart, s.isFavorite)
            // Bounce animation
            val bounce = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.heart_bounce)
            holder.heart.startAnimation(bounce)
        }
        holder.itemView.setOnClickListener { onClick(s) }
    }

    private fun updateHeartIcon(iv: ImageView, isFav: Boolean) {
        if (isFav) {
            iv.setImageResource(android.R.drawable.btn_star_big_on)
            iv.setColorFilter(Color.parseColor("#FF4081"))
        } else {
            iv.setImageResource(android.R.drawable.btn_star_big_off)
            iv.clearColorFilter()
        }
    }

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<SongHome>) {
        songs = newSongs.toMutableList()
        notifyDataSetChanged()
    }
}