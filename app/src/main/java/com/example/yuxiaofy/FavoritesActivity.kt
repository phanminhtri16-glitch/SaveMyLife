package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var tvCount: TextView

    // In a real app, load from Room database
    private val favoriteSongs = mutableListOf(
        SongHome("Unity", "TheFatRat", R.drawable.ic_launcher_background, true, "3:48"),
        SongHome("Infinite Power", "TheFatRat", R.drawable.ic_launcher_background, true, "3:55"),
        SongHome("Blinding Lights", "The Weeknd", R.drawable.ic_launcher_background, true, "3:20"),
        SongHome("Radioactive", "Imagine Dragons", R.drawable.ic_launcher_background, true, "3:06"),
        SongHome("River Flows in You", "Yiruma", R.drawable.ic_launcher_background, true, "3:52"),
        SongHome(
            "Leave The Door Open",
            "Silk Sonic",
            R.drawable.ic_launcher_background,
            true,
            "4:02"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        rvFavorites = findViewById(R.id.rvFavorites)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvCount = findViewById(R.id.tvFavCount)

        setupRecyclerView()
        setupBackButton()
        animateEntrance()
    }

    private fun setupRecyclerView() {
        rvFavorites.layoutManager = LinearLayoutManager(this)
        val adapter = HomeSongAdapter(favoriteSongs) { song ->
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvFavorites.adapter = adapter
        tvCount.text = "${favoriteSongs.size} bài hát yêu thích"
        layoutEmpty.visibility = if (favoriteSongs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun animateEntrance() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        rvFavorites.startAnimation(anim)
    }
}
