package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvFavorites: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var tvCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: HomeSongAdapter
    private val favoriteSongs = mutableListOf<SongHome>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvFavorites = findViewById(R.id.rvFavorites)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvCount = findViewById(R.id.tvFavCount)
        progressBar = findViewById(R.id.favProgressBar)

        setupRecyclerView()
        setupBackButton()
        animateEntrance()
        loadFavorites()
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: run {
            layoutEmpty.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            return
        }
        progressBar.visibility = View.VISIBLE

        db.collection("favorites").document(userId).collection("songs")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    progressBar.visibility = View.GONE
                    return@addSnapshotListener
                }
                val songIds = snapshot.documents.map { it.id }
                if (songIds.isEmpty()) {
                    progressBar.visibility = View.GONE
                    favoriteSongs.clear()
                    adapter.updateData(favoriteSongs)
                    tvCount.text = "0 bài hát yêu thích"
                    layoutEmpty.visibility = View.VISIBLE
                    return@addSnapshotListener
                }
                favoriteSongs.clear()
                var loadedCount = 0
                songIds.forEach { songId ->
                    db.collection("songs").document(songId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                favoriteSongs.add(SongHome(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    artist = doc.getString("artist") ?: "",
                                    imageRes = R.drawable.ic_launcher_background,
                                    isFavorite = true,
                                    duration = doc.getString("duration") ?: "3:00",
                                    audioUrl = doc.getString("audioUrl") ?: "",
                                    coverUrl = doc.getString("coverUrl") ?: "",
                                    category = doc.getString("category") ?: ""
                                ))
                            }
                            loadedCount++
                            if (loadedCount == songIds.size) {
                                progressBar.visibility = View.GONE
                                adapter.updateData(favoriteSongs)
                                tvCount.text = "${favoriteSongs.size} bài hát yêu thích"
                                layoutEmpty.visibility = if (favoriteSongs.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                }
            }
    }

    private fun setupRecyclerView() {
        rvFavorites.layoutManager = LinearLayoutManager(this)
        adapter = HomeSongAdapter(favoriteSongs) { song ->
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_ID", song.id)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
                putExtra("SONG_AUDIO_URL", song.audioUrl)
                putExtra("SONG_DURATION", song.duration)
            })
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvFavorites.adapter = adapter
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun animateEntrance() {
        rvFavorites.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up))
    }
}