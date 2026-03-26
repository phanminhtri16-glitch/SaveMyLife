package com.example.yuxiaofy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import database.AppDatabase
import database.DownloadedSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadsActivity : AppCompatActivity() {

    private lateinit var rvDownloads: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: DownloadsAdapter
    private val downloadedSongs = mutableListOf<DownloadedSong>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        rvDownloads = findViewById(R.id.rvDownloads)
        tvCount = findViewById(R.id.tvDlCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupRecyclerView()
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        loadDownloadedSongs()
    }

    @OptIn(UnstableApi::class)
    private fun setupRecyclerView() {
        rvDownloads.layoutManager = LinearLayoutManager(this)
        adapter = DownloadsAdapter(downloadedSongs, 
            onClick = { song ->
                // PHÁT NGAY: Chuyển sang MainActivity với chế độ LOCAL
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SONG_ID", song.id)
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_AUDIO_URL", song.localPath)
                    putExtra("SONG_COVER_URL", song.coverUrl)
                    putExtra("SONG_DURATION", song.duration)
                    putExtra("IS_LOCAL", true) // Quan trọng để MainActivity biết là nghe offline
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_fade, R.anim.fade_out)
            },
            onDelete = { song ->
                confirmDelete(song)
            }
        )
        rvDownloads.adapter = adapter
    }

    private fun confirmDelete(song: DownloadedSong) {
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc chắn muốn xóa \"${song.title}\" khỏi danh sách tải xuống?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteSong(song)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteSong(song: DownloadedSong) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DownloadsActivity)
            
            // 1. Xóa file vật lý trong máy
            try {
                val file = File(song.localPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Xóa khỏi database Room
            db.downloadedSongDao().delete(song)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@DownloadsActivity, "Đã xóa bài hát!", Toast.LENGTH_SHORT).show()
                loadDownloadedSongs() // Load lại danh sách
            }
        }
    }

    private fun loadDownloadedSongs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DownloadsActivity)
            val songs = db.downloadedSongDao().getAllDownloadedSongs()
            withContext(Dispatchers.Main) {
                downloadedSongs.clear()
                downloadedSongs.addAll(songs)
                adapter.notifyDataSetChanged()
                tvCount.text = "${songs.size} bài hát"
                layoutEmpty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}

class DownloadsAdapter(
    private val songs: List<DownloadedSong>,
    private val onClick: (DownloadedSong) -> Unit,
    private val onDelete: (DownloadedSong) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvHomeSongTitle)
        val artist: TextView = v.findViewById(R.id.tvHomeArtist)
        val duration: TextView = v.findViewById(R.id.tvDuration)
        val img: ImageView = v.findViewById(R.id.imgSongCover)
        val btnDownload: ImageView = v.findViewById(R.id.btnHomeDownload)
        val btnAction: ImageView = v.findViewById(R.id.btnHeart) // Sử dụng btnHeart làm nút xóa
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_home_song, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.title.text = s.title
        holder.artist.text = s.artist
        holder.duration.text = s.duration
        
        // Ẩn nút tải xuống vì đây là mục nhạc ĐÃ tải xuống
        holder.btnDownload.visibility = View.GONE
        
        // Đổi nút Heart thành nút Xóa (Thùng rác)
        holder.btnAction.setImageResource(android.R.drawable.ic_menu_delete)
        holder.btnAction.setColorFilter(Color.parseColor("#FF4444")) // Màu đỏ cho nút xóa

        Glide.with(holder.itemView.context)
            .load(s.coverUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(holder.img)

        holder.itemView.setOnClickListener { onClick(s) }
        holder.btnAction.setOnClickListener { onDelete(s) }
    }

    override fun getItemCount() = songs.size
}