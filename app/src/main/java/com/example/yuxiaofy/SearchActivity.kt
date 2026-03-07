package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var layoutEmpty: View
    private lateinit var searchAdapter: HomeSongAdapter

    private val allSongs = listOf(
        SongHome("Unity", "TheFatRat", R.drawable.ic_launcher_background, true, "3:48"),
        SongHome(
            "Monody",
            "TheFatRat ft. Laura Brehm",
            R.drawable.ic_launcher_background,
            false,
            "4:12"
        ),
        SongHome("Time Lapse", "TheFatRat", R.drawable.ic_launcher_background, false, "3:21"),
        SongHome(
            "The Calling",
            "TheFatRat ft. Laura Brehm",
            R.drawable.ic_launcher_background,
            false,
            "4:07"
        ),
        SongHome("Xenogenesis", "TheFatRat", R.drawable.ic_launcher_background, false, "5:01"),
        SongHome("Blinding Lights", "The Weeknd", R.drawable.ic_launcher_background, true, "3:20"),
        SongHome(
            "Stay",
            "Kid LAROI & Justin Bieber",
            R.drawable.ic_launcher_background,
            false,
            "2:21"
        ),
        SongHome("Believer", "Imagine Dragons", R.drawable.ic_launcher_background, false, "3:23"),
        SongHome("Radioactive", "Imagine Dragons", R.drawable.ic_launcher_background, true, "3:06"),
        SongHome("River Flows in You", "Yiruma", R.drawable.ic_launcher_background, true, "3:52"),
        SongHome("Weightless", "Marconi Union", R.drawable.ic_launcher_background, false, "8:09"),
        SongHome("Stronger", "Kanye West", R.drawable.ic_launcher_background, false, "5:11"),
        SongHome("HUMBLE.", "Kendrick Lamar", R.drawable.ic_launcher_background, false, "2:57"),
        SongHome("Peaches", "Justin Bieber", R.drawable.ic_launcher_background, false, "3:18"),
        SongHome("Experience", "Ludovico Einaudi", R.drawable.ic_launcher_background, false, "5:16")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etSearch = findViewById(R.id.etSearchMain)
        rvResults = findViewById(R.id.rvSearchResults)
        tvResultCount = findViewById(R.id.tvResultCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupRecyclerView()
        setupSearch()
        setupBackButton()
        animateEntrance()

        // Auto-focus search
        etSearch.requestFocus()
    }

    private fun setupRecyclerView() {
        rvResults.layoutManager = LinearLayoutManager(this)
        searchAdapter = HomeSongAdapter(mutableListOf()) { song ->
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvResults.adapter = searchAdapter

        tvResultCount.text = "${allSongs.size} bài hát"
        searchAdapter.updateData(allSongs)
        layoutEmpty.visibility = View.GONE
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase(Locale.getDefault()).trim()
                val filtered = if (query.isEmpty()) allSongs
                else allSongs.filter {
                    it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                }
                searchAdapter.updateData(filtered)
                tvResultCount.text = "${filtered.size} kết quả"
                layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun animateEntrance() {
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        rvResults.startAnimation(slideIn)
    }
}