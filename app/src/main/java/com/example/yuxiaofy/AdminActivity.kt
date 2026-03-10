package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val category: String = "",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val duration: String = ""
)

class AdminActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvSongs: RecyclerView
    private lateinit var adminAdapter: AdminSongAdapter
    private val songList = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val tvAdminName = findViewById<TextView>(R.id.tvAdminName)
        val btnAddSong = findViewById<Button>(R.id.btnAddSong)
        val btnLogout = findViewById<Button>(R.id.btnAdminLogout)
        val btnGoHome = findViewById<Button>(R.id.btnGoHome)
        rvSongs = findViewById(R.id.rvAdminSongs)

        val name = intent.getStringExtra("USER_NAME") ?: "Admin"
        tvAdminName.text = "Xin chào, $name 👋"

        // Setup RecyclerView
        rvSongs.layoutManager = LinearLayoutManager(this)
        adminAdapter = AdminSongAdapter(songList,
            onEdit = { song -> showSongDialog(song) },
            onDelete = { song -> confirmDelete(song) }
        )
        rvSongs.adapter = adminAdapter

        // Load danh sách bài hát
        loadSongs()

        // Thêm bài hát mới
        btnAddSong.setOnClickListener {
            showSongDialog(null)
        }

        // Đăng xuất
        btnLogout.setOnClickListener {
            auth.signOut()
            getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        // Vào trang Home (xem như user)
        btnGoHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun loadSongs() {
        db.collection("songs")
            .addSnapshotListener { snapshot, _ ->
                songList.clear()
                snapshot?.documents?.forEach { doc ->
                    val song = Song(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        category = doc.getString("category") ?: "",
                        audioUrl = doc.getString("audioUrl") ?: "",
                        coverUrl = doc.getString("coverUrl") ?: "",
                        duration = doc.getString("duration") ?: ""
                    )
                    songList.add(song)
                }
                adminAdapter.notifyDataSetChanged()
            }
    }

    private fun showSongDialog(song: Song?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_song, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etSongTitle)
        val etArtist = dialogView.findViewById<EditText>(R.id.etSongArtist)
        val etCategory = dialogView.findViewById<EditText>(R.id.etSongCategory)
        val etAudioUrl = dialogView.findViewById<EditText>(R.id.etAudioUrl)
        val etCoverUrl = dialogView.findViewById<EditText>(R.id.etCoverUrl)
        val etDuration = dialogView.findViewById<EditText>(R.id.etDuration)

        // Nếu là edit thì điền sẵn dữ liệu
        song?.let {
            etTitle.setText(it.title)
            etArtist.setText(it.artist)
            etCategory.setText(it.category)
            etAudioUrl.setText(it.audioUrl)
            etCoverUrl.setText(it.coverUrl)
            etDuration.setText(it.duration)
        }

        AlertDialog.Builder(this)
            .setTitle(if (song == null) "Thêm bài hát" else "Sửa bài hát")
            .setView(dialogView)
            .setPositiveButton(if (song == null) "Thêm" else "Lưu") { _, _ ->
                val title = etTitle.text.toString().trim()
                val artist = etArtist.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val audioUrl = etAudioUrl.text.toString().trim()
                val coverUrl = etCoverUrl.text.toString().trim()
                val duration = etDuration.text.toString().trim()

                if (title.isEmpty() || artist.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên bài hát và nghệ sĩ!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = hashMapOf(
                    "title" to title,
                    "artist" to artist,
                    "category" to category,
                    "audioUrl" to audioUrl,
                    "coverUrl" to coverUrl,
                    "duration" to duration
                )

                if (song == null) {
                    // Thêm mới
                    db.collection("songs").add(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Thêm bài hát thành công!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Cập nhật
                    db.collection("songs").document(song.id).set(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDelete(song: Song) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc muốn xóa \"${song.title}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("songs").document(song.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}

// Adapter cho Admin
class AdminSongAdapter(
    private val songs: MutableList<Song>,
    private val onEdit: (Song) -> Unit,
    private val onDelete: (Song) -> Unit
) : RecyclerView.Adapter<AdminSongAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvAdminSongTitle)
        val tvArtist: TextView = v.findViewById(R.id.tvAdminSongArtist)
        val tvCategory: TextView = v.findViewById(R.id.tvAdminCategory)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEditSong)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteSong)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.tvTitle.text = s.title
        holder.tvArtist.text = s.artist
        holder.tvCategory.text = s.category
        holder.btnEdit.setOnClickListener { onEdit(s) }
        holder.btnDelete.setOnClickListener { onDelete(s) }
    }

    override fun getItemCount() = songs.size
}