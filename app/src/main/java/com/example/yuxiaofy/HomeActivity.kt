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
import com.bumptech.glide.Glide
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
    private val allSongs = mutableListOf<SongHome>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = FirebaseFirestore.getInstance()

        rvSongs = findViewById(R.id.rvSongs)
        listTitle = findViewById(R.id.tvListTitle)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvUserName = findViewById(R.id.tvUserName)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 0..11 -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        tvUserName.text = prefs.getString("logged_name", "User")

        setupRecyclerView()
        setupNavigation()
        loadSongs()
    }

    private fun setupRecyclerView() {
        rvSongs.layoutManager = LinearLayoutManager(this)
        songAdapter = HomeSongAdapter(allSongs) { song ->
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_ID", song.id)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
                putExtra("SONG_AUDIO_URL", song.audioUrl)
                putExtra("SONG_DURATION", song.duration)
                putExtra("SONG_COVER_ART_URL", song.coverUrl)
            })
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvSongs.adapter = songAdapter
    }

    private fun loadSongs() {
        db.collection("songs").get().addOnSuccessListener { snapshot ->
            allSongs.clear()
            for (doc in snapshot.documents) {
                val song = SongHome(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    duration = doc.getString("duration") ?: "",
                    audioUrl = doc.getString("audioUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    category = doc.getString("category") ?: ""
                )
                allSongs.add(song)
            }
            songAdapter.updateData(allSongs)
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnNavSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.btnNavFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<View>(R.id.btnNavProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}

class HomeSongAdapter(
    private var songs: List<SongHome>,
    private val onClick: (SongHome) -> Unit
) : RecyclerView.Adapter<HomeSongAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvHomeSongTitle)
        val artist: TextView = v.findViewById(R.id.tvHomeArtist)
        val duration: TextView = v.findViewById(R.id.tvDuration)
        val img: ImageView = v.findViewById(R.id.imgSongCover)
        val heart: ImageView = v.findViewById(R.id.btnHeart)
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
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.ic_launcher_background)
        }
        updateHeartIcon(holder.heart, s.isFavorite)
        holder.heart.setOnClickListener {
            s.isFavorite = !s.isFavorite; updateHeartIcon(holder.heart, s.isFavorite)
            holder.heart.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.heart_bounce))
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

    fun updateData(newSongs: List<SongHome>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    override fun getItemCount() = songs.size
}