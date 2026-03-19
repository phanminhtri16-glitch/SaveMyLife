package com.example.yuxiaofy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.util.Locale

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val category: String = "",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val duration: String = ""
)

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val isAdmin: Boolean = false
)

class AdminActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var rvSongs: RecyclerView
    private lateinit var adminSongAdapter: AdminSongAdapter
    private val songList = mutableListOf<Song>()
    private val filteredSongList = mutableListOf<Song>()
    private var currentCategory = "Tất cả"

    private lateinit var rvUsers: RecyclerView
    private lateinit var adminUserAdapter: AdminUserAdapter
    private val userList = mutableListOf<AppUser>()
    private val filteredUserList = mutableListOf<AppUser>()
    private var currentUserRoleFilter = "Tất cả"

    private lateinit var tvTotalSongs: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var btnTabSongs: TextView
    private lateinit var btnTabUsers: TextView
    private lateinit var btnTabStats: TextView
    private lateinit var layoutSongs: View
    private lateinit var layoutUsers: View
    private lateinit var layoutStats: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bindViews()
        setupTabs()
        setupSongManagement()
        setupUserManagement()
        loadData()
    }

    private fun bindViews() {
        val tvAdminName = findViewById<TextView>(R.id.tvAdminName)
        val name = intent.getStringExtra("USER_NAME") ?: "Admin"
        tvAdminName.text = "Xin chào, $name"

        tvTotalSongs = findViewById(R.id.tvTotalSongs)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        btnTabSongs = findViewById(R.id.btnTabSongs)
        btnTabUsers = findViewById(R.id.btnTabUsers)
        btnTabStats = findViewById(R.id.btnTabStats)
        layoutSongs = findViewById(R.id.layoutSongs)
        layoutUsers = findViewById(R.id.layoutUsers)
        layoutStats = findViewById(R.id.layoutStats)
        rvSongs = findViewById(R.id.rvAdminSongs)
        rvUsers = findViewById(R.id.rvAdminUsers)

        findViewById<ImageButton>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        findViewById<Button>(R.id.btnGoHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun setupTabs() {
        showTab("songs")
        btnTabSongs.setOnClickListener { showTab("songs") }
        btnTabUsers.setOnClickListener { showTab("users") }
        btnTabStats.setOnClickListener { showTab("stats") }
    }

    private fun showTab(tab: String) {
        val views = listOf(layoutSongs, layoutUsers, layoutStats)
        views.forEach { 
            if (it.visibility == View.VISIBLE) {
                it.animate().alpha(0f).setDuration(200).withEndAction {
                    it.visibility = View.GONE
                }
            }
        }

        val activeView = when(tab) {
            "songs" -> layoutSongs
            "users" -> layoutUsers
            else -> layoutStats
        }

        activeView.postDelayed({
            activeView.visibility = View.VISIBLE
            activeView.alpha = 0f
            activeView.animate().alpha(1f).setDuration(300).start()
        }, 210)

        val activeColor = Color.parseColor("#BB86FC")
        val inactiveColor = Color.WHITE
        btnTabSongs.setTextColor(if (tab == "songs") activeColor else inactiveColor)
        btnTabUsers.setTextColor(if (tab == "users") activeColor else inactiveColor)
        btnTabStats.setTextColor(if (tab == "stats") activeColor else inactiveColor)
        
        btnTabSongs.setTypeface(null, if (tab == "songs") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        btnTabUsers.setTypeface(null, if (tab == "users") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        btnTabStats.setTypeface(null, if (tab == "stats") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun setupSongManagement() {
        rvSongs.layoutManager = LinearLayoutManager(this)
        adminSongAdapter = AdminSongAdapter(filteredSongList,
            onEdit = { showSongDialog(it) },
            onDelete = { confirmDeleteSong(it) }
        )
        rvSongs.adapter = adminSongAdapter

        findViewById<Button>(R.id.btnAddSong).setOnClickListener { showSongDialog(null) }

        findViewById<EditText>(R.id.etSearchSong).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSongs(s.toString(), currentCategory)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val categories = arrayOf("Tất cả", "Chill", "Workout", "Focus", "RB")
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        
        // Sử dụng layout tùy chỉnh spinner_item_admin để chữ màu trắng nổi bật
        val adapter = ArrayAdapter(this, R.layout.spinner_item_admin, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories[position]
                filterSongs(findViewById<EditText>(R.id.etSearchSong).text.toString(), currentCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterSongs(query: String, category: String) {
        val q = query.lowercase(Locale.getDefault()).trim()
        filteredSongList.clear()
        filteredSongList.addAll(songList.filter { song ->
            val matchQuery = q.isEmpty() || song.title.lowercase().contains(q) || song.artist.lowercase().contains(q)
            val matchCategory = category == "Tất cả" || song.category.equals(category, ignoreCase = true)
            matchQuery && matchCategory
        })
        adminSongAdapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvSongCount).text = "${filteredSongList.size} bài hát"
    }

    private fun showSongDialog(song: Song?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_song, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etSongTitle)
        val etArtist = dialogView.findViewById<EditText>(R.id.etSongArtist)
        val etCategory = dialogView.findViewById<EditText>(R.id.etSongCategory)
        val etAudioUrl = dialogView.findViewById<EditText>(R.id.etAudioUrl)
        val etCoverUrl = dialogView.findViewById<EditText>(R.id.etCoverUrl)
        val etDuration = dialogView.findViewById<EditText>(R.id.etDuration)
        song?.let {
            etTitle.setText(it.title); etArtist.setText(it.artist)
            etCategory.setText(it.category); etAudioUrl.setText(it.audioUrl)
            etCoverUrl.setText(it.coverUrl); etDuration.setText(it.duration)
        }
        
        // Áp dụng style AdminDialog để có background tối và nút tím
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle(if (song == null) "Thêm bài hát" else "Sửa bài hát")
            .setView(dialogView)
            .setPositiveButton(if (song == null) "Thêm" else "Lưu") { _, _ ->
                val title = etTitle.text.toString().trim()
                val artist = etArtist.text.toString().trim()
                if (title.isEmpty() || artist.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên và nghệ sĩ!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val data = hashMapOf(
                    "title" to title, "artist" to artist,
                    "category" to etCategory.text.toString().trim(),
                    "audioUrl" to etAudioUrl.text.toString().trim(),
                    "coverUrl" to etCoverUrl.text.toString().trim(),
                    "duration" to etDuration.text.toString().trim()
                )
                if (song == null) {
                    db.collection("songs").add(data).addOnSuccessListener {
                        Toast.makeText(this, "Thêm bài hát thành công!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    db.collection("songs").document(song.id).set(data).addOnSuccessListener {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun confirmDeleteSong(song: Song) {
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("Xóa bài hát")
            .setMessage("Xóa \"${song.title}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("songs").document(song.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun setupUserManagement() {
        rvUsers.layoutManager = LinearLayoutManager(this)
        adminUserAdapter = AdminUserAdapter(filteredUserList,
            onToggleAdmin = { toggleAdminRole(it) },
            onDelete = { confirmDeleteUser(it) }
        )
        rvUsers.adapter = adminUserAdapter

        findViewById<EditText>(R.id.etSearchUser).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { 
                filterUsers(s.toString(), currentUserRoleFilter) 
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val roles = arrayOf("Tất cả", "Admin", "User")
        val spinner = findViewById<Spinner>(R.id.spinnerUserRole)
        val adapter = ArrayAdapter(this, R.layout.spinner_item_admin, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentUserRoleFilter = roles[position]
                filterUsers(findViewById<EditText>(R.id.etSearchUser).text.toString(), currentUserRoleFilter)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterUsers(query: String, role: String) {
        val q = query.lowercase(Locale.getDefault()).trim()
        filteredUserList.clear()
        filteredUserList.addAll(userList.filter { user ->
            val matchQuery = q.isEmpty() || user.name.lowercase().contains(q) || user.email.lowercase().contains(q)
            val matchRole = when(role) {
                "Admin" -> user.isAdmin
                "User" -> !user.isAdmin
                else -> true
            }
            matchQuery && matchRole
        })
        adminUserAdapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvUserCount).text = "${filteredUserList.size} người dùng"
    }

    private fun toggleAdminRole(user: AppUser) {
        val newRole = !user.isAdmin
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("Đổi quyền")
            .setMessage("Đặt ${user.name} thành ${if (newRole) "Admin" else "User"}?")
            .setPositiveButton("Xác nhận") { _, _ ->
                db.collection("users").document(user.uid).update("isAdmin", newRole)
                    .addOnSuccessListener { Toast.makeText(this, "Đã đổi quyền!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun confirmDeleteUser(user: AppUser) {
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("Xóa tài khoản")
            .setMessage("Xóa tài khoản \"${user.email}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("users").document(user.uid).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun loadData() {
        db.collection("songs").addSnapshotListener { snapshot, _ ->
            songList.clear()
            snapshot?.documents?.forEach { doc ->
                songList.add(Song(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    category = doc.getString("category") ?: "",
                    audioUrl = doc.getString("audioUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    duration = doc.getString("duration") ?: ""
                ))
            }
            filterSongs(findViewById<EditText>(R.id.etSearchSong).text.toString(), currentCategory)
            updateStats()
        }

        db.collection("users").addSnapshotListener { snapshot, _ ->
            userList.clear()
            snapshot?.documents?.forEach { doc ->
                userList.add(AppUser(
                    uid = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    isAdmin = doc.getBoolean("isAdmin") ?: false
                ))
            }
            filterUsers(findViewById<EditText>(R.id.etSearchUser).text.toString(), currentUserRoleFilter)
            updateStats()
        }
    }

    private fun updateStats() {
        val totalSongs = songList.size
        tvTotalSongs.text = totalSongs.toString()
        val totalUsers = userList.size
        tvTotalUsers.text = totalUsers.toString()

        val chillCount = songList.count { it.category.equals("Chill", ignoreCase = true) }
        val workoutCount = songList.count { it.category.equals("Workout", ignoreCase = true) }
        val focusCount = songList.count { it.category.equals("Focus", ignoreCase = true) }
        val rbCount = songList.count { it.category.equals("RB", ignoreCase = true) }
        
        findViewById<TextView>(R.id.tvStatChill).text = "$chillCount bài"
        findViewById<TextView>(R.id.tvStatWorkout).text = "$workoutCount bài"
        findViewById<TextView>(R.id.tvStatFocus).text = "$focusCount bài"
        findViewById<TextView>(R.id.tvStatRB).text = "$rbCount bài"

        if (totalSongs > 0) {
            findViewById<ProgressBar>(R.id.pbChill).progress = (chillCount * 100 / totalSongs)
            findViewById<ProgressBar>(R.id.pbWorkout).progress = (workoutCount * 100 / totalSongs)
            findViewById<ProgressBar>(R.id.pbFocus).progress = (focusCount * 100 / totalSongs)
            findViewById<ProgressBar>(R.id.pbRB).progress = (rbCount * 100 / totalSongs)
        }

        val adminCount = userList.count { it.isAdmin }
        val regularUserCount = userList.count { !it.isAdmin }
        findViewById<TextView>(R.id.tvStatAdmins).text = "Admin: $adminCount"
        findViewById<TextView>(R.id.tvStatUsers).text = "User: $regularUserCount"

        if (totalUsers > 0) {
            findViewById<ProgressBar>(R.id.pbUserRatio).progress = (adminCount * 100 / totalUsers)
        }
    }
}

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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_song, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.tvTitle.text = s.title; holder.tvArtist.text = s.artist; holder.tvCategory.text = s.category
        holder.btnEdit.setOnClickListener { onEdit(s) }
        holder.btnDelete.setOnClickListener { onDelete(s) }
    }
    override fun getItemCount() = songs.size
}

class AdminUserAdapter(
    private val users: MutableList<AppUser>,
    private val onToggleAdmin: (AppUser) -> Unit,
    private val onDelete: (AppUser) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvUserName)
        val tvEmail: TextView = v.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = v.findViewById(R.id.tvUserRole)
        val btnToggle: Button = v.findViewById(R.id.btnToggleAdmin)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteUser)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        holder.tvName.text = u.name; holder.tvEmail.text = u.email
        holder.tvRole.text = if (u.isAdmin) "Admin" else "User"
        holder.tvRole.setTextColor(android.graphics.Color.parseColor(if (u.isAdmin) "#BB86FC" else "#03DAC6"))
        holder.btnToggle.text = if (u.isAdmin) "Xoá quyền" else "Cấp quyền"
        holder.btnToggle.setOnClickListener { onToggleAdmin(u) }
        holder.btnDelete.setOnClickListener { onDelete(u) }
    }
    override fun getItemCount() = users.size
}