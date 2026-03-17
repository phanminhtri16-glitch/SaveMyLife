package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/* ══════════════════════════════════════════════════════════
   Data models
══════════════════════════════════════════════════════════ */
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val category: String = "",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val duration: String = ""
)

data class UserModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = 0L
)

/* ══════════════════════════════════════════════════════════
   AdminActivity
══════════════════════════════════════════════════════════ */
class AdminActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Songs
    private lateinit var rvSongs: RecyclerView
    private lateinit var songAdapter: AdminSongAdapter
    private val songList = mutableListOf<Song>()
    private val songListFull = mutableListOf<Song>()

    // Users
    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: AdminUserAdapter
    private val userList = mutableListOf<UserModel>()
    private val userListFull = mutableListOf<UserModel>()

    // Panels
    private lateinit var panelSongs: View
    private lateinit var panelUsers: View
    private lateinit var panelDashboard: View

    // Stats
    private lateinit var tvStatSongs: TextView
    private lateinit var tvStatUsers: TextView
    private lateinit var tvStatAdmins: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bindViews()
        setupTabs()
        setupQuickAccess()
        setupSongSearch()
        setupUserSearch()
        loadSongs()
        loadUsers()
        setupFab()
        setupLogout()
        setupGoHome()
    }

    private fun bindViews() {
        panelDashboard = findViewById(R.id.panelDashboard)
        panelSongs = findViewById(R.id.panelSongs)
        panelUsers = findViewById(R.id.panelUsers)
        tvStatSongs = findViewById(R.id.tvStatSongs)
        tvStatUsers = findViewById(R.id.tvStatUsers)
        tvStatAdmins = findViewById(R.id.tvStatAdmins)

        val name = intent.getStringExtra("USER_NAME") ?: "Admin"
        findViewById<TextView>(R.id.tvAdminName).text = "Hello, $name 👋"

        // Songs RecyclerView
        rvSongs = findViewById(R.id.rvAdminSongs)
        rvSongs.layoutManager = LinearLayoutManager(this)
        songAdapter = AdminSongAdapter(
            songList,
            onEdit = { showSongDialog(it) },
            onDelete = { confirmDeleteSong(it) }
        )
        rvSongs.adapter = songAdapter

        // Users RecyclerView
        rvUsers = findViewById(R.id.rvAdminUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = AdminUserAdapter(
            userList,
            onEdit = { showUserDialog(it) },
            onDelete = { confirmDeleteUser(it) },
            onToggleRole = { toggleUserRole(it) }
        )
        rvUsers.adapter = userAdapter
    }

    /* ── Tabs ─────────────────────────────────────────────────── */
    private fun setupTabs() {
        val tabs = findViewById<TabLayout>(R.id.adminTabs)
        
        // Add tabs programmatically if not in XML
        if (tabs.tabCount == 0) {
            tabs.addTab(tabs.newTab().setText("Dashboard"))
            tabs.addTab(tabs.newTab().setText("Songs"))
            tabs.addTab(tabs.newTab().setText("Users"))
        }

        showPanel(0)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showPanel(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupQuickAccess() {
        val cardManageSongs = findViewById<CardView>(R.id.cardManageSongs)
        val cardManageUsers = findViewById<CardView>(R.id.cardManageUsers)
        val tabs = findViewById<TabLayout>(R.id.adminTabs)

        cardManageSongs.setOnClickListener {
            tabs.getTabAt(1)?.select()
        }

        cardManageUsers.setOnClickListener {
            tabs.getTabAt(2)?.select()
        }
    }

    private fun showPanel(index: Int) {
        panelDashboard.visibility = if (index == 0) View.VISIBLE else View.GONE
        panelSongs.visibility = if (index == 1) View.VISIBLE else View.GONE
        panelUsers.visibility = if (index == 2) View.VISIBLE else View.GONE

        val fab =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
        fab.visibility = if (index == 0) View.GONE else View.VISIBLE
    }

    /* ── FAB ──────────────────────────────────────────────────── */
    private fun setupFab() {
        val fab =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
        fab.setOnClickListener {
            val tabs = findViewById<TabLayout>(R.id.adminTabs)
            if (tabs.selectedTabPosition == 1) showSongDialog(null)
            else if (tabs.selectedTabPosition == 2) showUserCreateDialog()
        }
    }

    /* ── Search ───────────────────────────────────────────────── */
    private fun setupSongSearch() {
        findViewById<EditText>(R.id.etSearchSongs).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                val filtered = if (q.isEmpty()) songListFull
                else songListFull.filter {
                    it.title.lowercase().contains(q) || it.artist.lowercase()
                        .contains(q) || it.category.lowercase().contains(q)
                }
                songAdapter.updateData(filtered)
            }
        })
    }

    private fun setupUserSearch() {
        findViewById<EditText>(R.id.etSearchUsers).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                val filtered = if (q.isEmpty()) userListFull
                else userListFull.filter {
                    it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
                }
                userAdapter.updateData(filtered)
            }
        })
    }

    /* ── Load Data ────────────────────────────────────────────── */
    private fun loadSongs() {
        db.collection("songs").addSnapshotListener { snap, _ ->
            songList.clear(); songListFull.clear()
            snap?.documents?.forEach { doc ->
                val s = Song(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    category = doc.getString("category") ?: "",
                    audioUrl = doc.getString("audioUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    duration = doc.getString("duration") ?: ""
                )
                songList.add(s); songListFull.add(s)
            }
            songAdapter.notifyDataSetChanged()
            tvStatSongs.text = songList.size.toString()
            updateEmptyState(R.id.tvEmptySongs, R.id.rvAdminSongs, songList.isEmpty())
        }
    }

    private fun loadUsers() {
        db.collection("users").addSnapshotListener { snap, _ ->
            userList.clear(); userListFull.clear()
            var adminCount = 0
            snap?.documents?.forEach { doc ->
                val u = UserModel(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    isAdmin = doc.getBoolean("isAdmin") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
                if (u.isAdmin) adminCount++
                userList.add(u); userListFull.add(u)
            }
            userAdapter.notifyDataSetChanged()
            tvStatUsers.text = userList.size.toString()
            tvStatAdmins.text = adminCount.toString()
            updateEmptyState(R.id.tvEmptyUsers, R.id.rvAdminUsers, userList.isEmpty())
        }
    }

    private fun updateEmptyState(emptyId: Int, rvId: Int, isEmpty: Boolean) {
        findViewById<TextView>(emptyId).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(rvId).visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /* ══ SONG CRUD ══════════════════════════════════════════════ */
    private fun showSongDialog(song: Song?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_song_form, null)
        val etTitle = view.findViewById<EditText>(R.id.etSongTitle)
        val etArtist = view.findViewById<EditText>(R.id.etSongArtist)
        val etCategory = view.findViewById<EditText>(R.id.etSongCategory)
        val etAudio = view.findViewById<EditText>(R.id.etAudioUrl)
        val etCover = view.findViewById<EditText>(R.id.etCoverUrl)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)

        song?.let {
            etTitle.setText(it.title); etArtist.setText(it.artist)
            etCategory.setText(it.category); etAudio.setText(it.audioUrl)
            etCover.setText(it.coverUrl); etDuration.setText(it.duration)
        }

        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle(if (song == null) "➕  Add New Song" else "✏️  Edit Song")
            .setView(view)
            .setPositiveButton(if (song == null) "Add" else "Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val artist = etArtist.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val audio = etAudio.text.toString().trim()
                val cover = etCover.text.toString().trim()
                val duration = etDuration.text.toString().trim()

                if (title.isEmpty() || artist.isEmpty()) {
                    Toast.makeText(this, "Title and Artist are required!", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }
                val data = hashMapOf(
                    "title" to title, "artist" to artist,
                    "category" to category, "audioUrl" to audio,
                    "coverUrl" to cover, "duration" to duration
                )
                if (song == null) {
                    db.collection("songs").add(data).addOnSuccessListener {
                        toast("✓ Song added")
                    }
                } else {
                    db.collection("songs").document(song.id).set(data).addOnSuccessListener {
                        toast("✓ Song updated")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSong(song: Song) {
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("🗑️  Delete Song")
            .setMessage("Delete \"${song.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("songs").document(song.id).delete()
                    .addOnSuccessListener { toast("✓ Song deleted") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* ══ USER CRUD ══════════════════════════════════════════════ */
    private fun showUserCreateDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_user_form, null)
        val etName = view.findViewById<EditText>(R.id.etUserName)
        val etEmail = view.findViewById<EditText>(R.id.etUserEmail)
        val etPass = view.findViewById<EditText>(R.id.etUserPassword)
        val swAdmin = view.findViewById<Switch>(R.id.swIsAdmin)

        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("➕  Create User")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString().trim()
                val isAdmin = swAdmin.isChecked

                if (name.isEmpty() || email.isEmpty() || pass.length < 6) {
                    toast("Name, Email and Password (min 6 chars) are required"); return@setPositiveButton
                }
                // Create Firebase Auth user then Firestore doc
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val data = hashMapOf(
                            "name" to name, "email" to email,
                            "isAdmin" to isAdmin, "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(uid).set(data)
                            .addOnSuccessListener { toast("✓ User created") }
                    }
                    .addOnFailureListener { e -> toast("Error: ${e.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserDialog(user: UserModel) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_user_edit_form, null)
        val etName = view.findViewById<EditText>(R.id.etEditUserName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEditUserEmail)
        val swAdmin = view.findViewById<Switch>(R.id.swEditIsAdmin)

        etName.setText(user.name)
        tvEmail.text = user.email
        swAdmin.isChecked = user.isAdmin

        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("✏️  Edit User")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newIsAdmin = swAdmin.isChecked
                if (newName.isEmpty()) {
                    toast("Name cannot be empty"); return@setPositiveButton
                }

                db.collection("users").document(user.id)
                    .update("name", newName, "isAdmin", newIsAdmin)
                    .addOnSuccessListener { toast("✓ User updated") }
                    .addOnFailureListener { e -> toast("Error: ${e.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteUser(user: UserModel) {
        val currentUid = auth.currentUser?.uid
        if (user.id == currentUid) {
            toast("⚠️ You cannot delete your own account here"); return
        }
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("🗑️  Delete User")
            .setMessage("Delete \"${user.name}\" (${user.email})?\n\nNote: This removes the Firestore record. Firebase Auth account must be deleted separately.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(user.id).delete()
                    .addOnSuccessListener { toast("✓ User record deleted") }
                    .addOnFailureListener { e -> toast("Error: ${e.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleUserRole(user: UserModel) {
        val newRole = !user.isAdmin
        val label = if (newRole) "Admin" else "User"
        AlertDialog.Builder(this, R.style.AdminDialog)
            .setTitle("🔐  Change Role")
            .setMessage("Set \"${user.name}\" as $label?")
            .setPositiveButton("Confirm") { _, _ ->
                db.collection("users").document(user.id)
                    .update("isAdmin", newRole)
                    .addOnSuccessListener { toast("✓ Role set to $label") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* ── Toolbar buttons ──────────────────────────────────────── */
    private fun setupLogout() {
        findViewById<ImageView>(R.id.btnAdminLogout).setOnClickListener {
            AlertDialog.Builder(this, R.style.AdminDialog)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    auth.signOut()
                    getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupGoHome() {
        findViewById<ImageView>(R.id.btnGoHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

/* ══════════════════════════════════════════════════════════
   Song Adapter
══════════════════════════════════════════════════════════ */
class AdminSongAdapter(
    private var songs: MutableList<Song>,
    private val onEdit: (Song) -> Unit,
    private val onDelete: (Song) -> Unit
) : RecyclerView.Adapter<AdminSongAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvAdminSongTitle)
        val tvArtist: TextView = v.findViewById(R.id.tvAdminSongArtist)
        val tvCategory: TextView = v.findViewById(R.id.tvAdminCategory)
        val tvDuration: TextView = v.findViewById(R.id.tvAdminDuration)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEditSong)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteSong)
        val categoryBadge: TextView = v.findViewById(R.id.tvAdminCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_song, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = songs[position]
        holder.tvTitle.text = s.title
        holder.tvArtist.text = s.artist
        holder.tvCategory.text = s.category.ifEmpty { "—" }
        holder.tvDuration.text = s.duration.ifEmpty { "--:--" }

        // Category badge colour
        val color = when (s.category.lowercase()) {
            "chill" -> "#1E88E5"
            "workout" -> "#E53935"
            "focus" -> "#43A047"
            "rb" -> "#8E24AA"
            else -> "#424242"
        }
        holder.tvCategory.background?.setTint(android.graphics.Color.parseColor(color))

        holder.btnEdit.setOnClickListener { onEdit(s) }
        holder.btnDelete.setOnClickListener { onDelete(s) }
    }

    override fun getItemCount() = songs.size

    fun updateData(list: List<Song>) {
        songs = list.toMutableList()
        notifyDataSetChanged()
    }
}

/* ══════════════════════════════════════════════════════════
   User Adapter
══════════════════════════════════════════════════════════ */
class AdminUserAdapter(
    private var users: MutableList<UserModel>,
    private val onEdit: (UserModel) -> Unit,
    private val onDelete: (UserModel) -> Unit,
    private val onToggleRole: (UserModel) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvAvatar: TextView = v.findViewById(R.id.tvUserAvatar)
        val tvName: TextView = v.findViewById(R.id.tvUserName)
        val tvEmail: TextView = v.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = v.findViewById(R.id.tvUserRole)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEditUser)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteUser)
        val btnRole: ImageButton = v.findViewById(R.id.btnToggleRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        holder.tvAvatar.text = u.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.tvName.text = u.name
        holder.tvEmail.text = u.email

        if (u.isAdmin) {
            holder.tvRole.text = "Admin"
            holder.tvRole.setTextColor("#BB86FC".toColorInt())
            holder.tvRole.background?.setTint("#2A1A4E".toColorInt())
            holder.btnRole.setImageResource(android.R.drawable.ic_lock_lock)
            holder.tvAvatar.background?.setTint("#BB86FC".toColorInt())
        } else {
            holder.tvRole.text = "User"
            holder.tvRole.setTextColor("#4FC3F7".toColorInt())
            holder.tvRole.background?.setTint("#0D2A3E".toColorInt())
            holder.btnRole.setImageResource(android.R.drawable.ic_lock_idle_lock)
            holder.tvAvatar.background?.setTint("#1E88E5".toColorInt())
        }

        holder.btnEdit.setOnClickListener { onEdit(u) }
        holder.btnDelete.setOnClickListener { onDelete(u) }
        holder.btnRole.setOnClickListener { onToggleRole(u) }
    }

    override fun getItemCount() = users.size

    fun updateData(list: List<UserModel>) {
        users = list.toMutableList()
        notifyDataSetChanged()
    }
}