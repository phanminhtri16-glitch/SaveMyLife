package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var layoutEmpty: View
    private lateinit var searchAdapter: HomeSongAdapter
    private val allSongs = mutableListOf<SongHome>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        db = FirebaseFirestore.getInstance()
        etSearch = findViewById(R.id.etSearchMain)
        rvResults = findViewById(R.id.rvSearchResults)
        tvResultCount = findViewById(R.id.tvResultCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupRecyclerView()
        setupSearch()
        setupBackButton()
        animateEntrance()
        loadSongsFromFirestore()
        etSearch.requestFocus()
    }

    private fun loadSongsFromFirestore() {
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
        }
    }

    private fun filterSongs(query: String) {
        if (query.isEmpty()) {
            searchAdapter.updateData(emptyList())
            tvResultCount.text = "0 kết quả"
            layoutEmpty.visibility = View.VISIBLE
            return
        }

        val filtered = allSongs.filter {
            it.title.lowercase(Locale.getDefault()).contains(query) ||
                    it.artist.lowercase(Locale.getDefault()).contains(query)
        }
        searchAdapter.updateData(filtered)
        tvResultCount.text = "${filtered.size} kết quả"
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSongs(s.toString().lowercase(Locale.getDefault()).trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        rvResults.layoutManager = LinearLayoutManager(this)
        searchAdapter = HomeSongAdapter(mutableListOf()) { song ->
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
        rvResults.adapter = searchAdapter
        layoutEmpty.visibility = View.GONE
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun animateEntrance() {
        rvResults.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up))
    }
}