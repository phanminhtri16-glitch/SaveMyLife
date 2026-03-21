package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import database.AppDatabase
import database.ListenHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var tvCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: HistoryAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        auth = FirebaseAuth.getInstance()

        rvHistory = findViewById(R.id.rvHistory)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvCount = findViewById(R.id.tvHistoryCount)
        progressBar = findViewById(R.id.historyProgressBar)

        setupRecyclerView()
        setupBackButton()
        setupClearButton()
        loadHistory()
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: run {
            layoutEmpty.visibility = View.VISIBLE
            return
        }
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@HistoryActivity)
            val history = db.listenHistoryDao().getHistoryByUser(userId)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                adapter.updateData(history)
                tvCount.text = "${history.size} bài đã nghe"
                layoutEmpty.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(mutableListOf()) { history ->
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("SONG_ID", history.songId)
                putExtra("SONG_TITLE", history.title)
                putExtra("SONG_ARTIST", history.artist)
                putExtra("SONG_AUDIO_URL", history.audioUrl)
                putExtra("SONG_COVER_URL", history.coverUrl)
            })
            overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
        }
        rvHistory.adapter = adapter
        rvHistory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up))
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupClearButton() {
        findViewById<TextView>(R.id.btnClearHistory).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử")
                .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử nghe không?")
                .setPositiveButton("Xóa") { _, _ ->
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@HistoryActivity)
                            .listenHistoryDao().clearHistoryByUser(userId)
                        withContext(Dispatchers.Main) {
                            adapter.updateData(emptyList())
                            tvCount.text = "0 bài đã nghe"
                            layoutEmpty.visibility = View.VISIBLE
                            Toast.makeText(this@HistoryActivity, "Đã xóa lịch sử!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
}

class HistoryAdapter(
    private var items: MutableList<ListenHistory>,
    private val onClick: (ListenHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvHistoryTitle)
        val tvArtist: TextView = v.findViewById(R.id.tvHistoryArtist)
        val tvTime: TextView = v.findViewById(R.id.tvHistoryTime)
        val imgCover: ImageView = v.findViewById(R.id.imgHistoryCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]
        holder.tvTitle.text = h.title
        holder.tvArtist.text = h.artist
        holder.tvTime.text = formatTime(h.listenedAt)

        if (h.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(h.coverUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.imgCover)
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_launcher_background)
        }

        holder.itemView.setOnClickListener { onClick(h) }
    }

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Vừa nghe"
            diff < 3_600_000 -> "${diff / 60_000} phút trước"
            diff < 86_400_000 -> "${diff / 3_600_000} giờ trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: List<ListenHistory>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}