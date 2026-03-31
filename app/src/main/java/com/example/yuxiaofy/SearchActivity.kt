package com.example.yuxiaofy

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import database.AppDatabase
import database.DownloadedSong
import database.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var layoutEmpty: View
    private lateinit var progressBar: ProgressBar
    private lateinit var searchAdapter: HomeSongAdapter

    private lateinit var layoutSearchResults: View
    private lateinit var layoutSearchHistory: View
    private lateinit var rvSearchHistory: RecyclerView
    private lateinit var btnClearHistory: TextView
    private lateinit var historyAdapter: SearchHistoryAdapter

    private val allSongs = mutableListOf<SongHome>()
    private lateinit var db: FirebaseFirestore
    private lateinit var roomDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        db = FirebaseFirestore.getInstance()
        roomDb = AppDatabase.getDatabase(this)

        etSearch = findViewById(R.id.etSearchMain)
        rvResults = findViewById(R.id.rvSearchResults)
        tvResultCount = findViewById(R.id.tvResultCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.searchProgressBar)

        layoutSearchResults = findViewById(R.id.layoutSearchResults)
        layoutSearchHistory = findViewById(R.id.layoutSearchHistory)
        rvSearchHistory = findViewById(R.id.rvSearchHistory)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        setupRecyclerView()
        setupHistoryRecyclerView()
        setupSearch()
        setupBackButton()
        animateEntrance()
        loadSongsFromFirestore()

        etSearch.requestFocus()
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val history = roomDb.searchHistoryDao().getRecentSearches()
            withContext(Dispatchers.Main) {
                historyAdapter.updateData(history)
                toggleHistoryView(etSearch.text.toString().isEmpty() && history.isNotEmpty() && etSearch.hasFocus())
            }
        }
    }

    private fun saveSearchQuery(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                roomDb.searchHistoryDao().deleteByQuery(q)
                roomDb.searchHistoryDao().insert(SearchHistory(query = q))
                loadSearchHistory()
            }
        }
    }

    private fun toggleHistoryView(showHistory: Boolean) {
        if (showHistory) {
            layoutSearchHistory.visibility = View.VISIBLE
            layoutSearchResults.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        } else {
            layoutSearchHistory.visibility = View.GONE
            layoutSearchResults.visibility = View.VISIBLE
        }
    }

    private fun loadSongsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        db.collection("songs").get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                allSongs.clear()
                for (doc in snapshot.documents) {
                    allSongs.add(
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
                val query = etSearch.text.toString().lowercase(Locale.getDefault()).trim()
                val display = if (query.isEmpty()) allSongs
                else allSongs.filter {
                    it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                }
                searchAdapter.updateData(display)
                tvResultCount.text = "${display.size} bài hát"
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Không thể tải danh sách nhạc", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        rvResults.layoutManager = LinearLayoutManager(this)
        searchAdapter = HomeSongAdapter(mutableListOf(),
            onItemClick = { song ->
                saveSearchQuery(etSearch.text.toString())
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
            onDownloadClick = { song -> downloadSong(song) },
            onFavoriteClick = { song, isFav ->
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@HomeSongAdapter
                val ref = db.collection("favorites").document(uid).collection("songs").document(song.id)
                if (isFav) ref.set(mapOf("addedAt" to System.currentTimeMillis())) else ref.delete()
            }
        )
        rvResults.adapter = searchAdapter
    }

    private fun downloadSong(song: SongHome) {
        if (song.audioUrl.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đường dẫn tải!", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val dbLocal = AppDatabase.getDatabase(this@SearchActivity)
            val existing = dbLocal.downloadedSongDao().getDownloadedSongById(song.id)
            if (existing != null) {
                withContext(Dispatchers.Main) { Toast.makeText(this@SearchActivity, "Đã tải về rồi!", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            withContext(Dispatchers.Main) { Toast.makeText(this@SearchActivity, "Bắt đầu tải: ${song.title}", Toast.LENGTH_SHORT).show() }

            try {
                val fileName = "${song.id}.mp3"
                val request = DownloadManager.Request(Uri.parse(song.audioUrl))
                    .setTitle(song.title)
                    .setDestinationInExternalFilesDir(this@SearchActivity, Environment.DIRECTORY_MUSIC, fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                val localPath = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName).absolutePath
                val downloadedSong = DownloadedSong(
                    id = song.id, title = song.title, artist = song.artist,
                    audioUrl = song.audioUrl, localPath = localPath, coverUrl = song.coverUrl, duration = song.duration
                )
                dbLocal.downloadedSongDao().insert(downloadedSong)

                withContext(Dispatchers.Main) { Toast.makeText(this@SearchActivity, "Đã thêm vào danh sách tải xuống!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@SearchActivity, "Lỗi tải: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun setupHistoryRecyclerView() {
        rvSearchHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = SearchHistoryAdapter(mutableListOf(),
            onClick = { historyQuery ->
                etSearch.setText(historyQuery)
                etSearch.setSelection(historyQuery.length)
                saveSearchQuery(historyQuery)
            },
            onDelete = { historyQuery ->
                lifecycleScope.launch(Dispatchers.IO) {
                    roomDb.searchHistoryDao().deleteByQuery(historyQuery)
                    loadSearchHistory()
                }
            }
        )
        rvSearchHistory.adapter = historyAdapter

        btnClearHistory.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                roomDb.searchHistoryDao().clearAll()
                loadSearchHistory()
            }
        }
    }

    private fun setupSearch() {
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            val query = etSearch.text.toString().trim()
            toggleHistoryView(hasFocus && query.isEmpty() && historyAdapter.itemCount > 0)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase(Locale.getDefault()).trim()

                if (query.isEmpty()) {
                    toggleHistoryView(etSearch.hasFocus() && historyAdapter.itemCount > 0)
                    searchAdapter.updateData(allSongs)
                    tvResultCount.text = "${allSongs.size} kết quả"
                    layoutEmpty.visibility = if (allSongs.isEmpty() && layoutSearchResults.visibility == View.VISIBLE) View.VISIBLE else View.GONE
                } else {
                    toggleHistoryView(false)
                    val filtered = allSongs.filter {
                        it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                    }
                    searchAdapter.updateData(filtered)
                    tvResultCount.text = "${filtered.size} kết quả"
                    layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                saveSearchQuery(etSearch.text.toString())
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                etSearch.clearFocus()
                true
            } else false
        }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun animateEntrance() {
        rvResults.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
    }
}

class SearchHistoryAdapter(
    private var items: MutableList<SearchHistory>,
    private val onClick: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvQuery: TextView = v.findViewById(R.id.tvHistoryQuery)
        val btnDelete: ImageView = v.findViewById(R.id.btnDeleteHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_search_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]
        holder.tvQuery.text = h.query
        holder.itemView.setOnClickListener { onClick(h.query) }
        holder.btnDelete.setOnClickListener { onDelete(h.query) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<SearchHistory>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}