package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAvatar = findViewById<TextView>(R.id.tvAvatarLetter)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvFavCount = findViewById<TextView>(R.id.tvStatFav)
        val tvPlaysCount = findViewById<TextView>(R.id.tvStatPlays)
        val tvHoursCount = findViewById<TextView>(R.id.tvStatHours)

        val rowEditProfile = findViewById<View>(R.id.rowEditProfile)
        val rowChangePassword = findViewById<View>(R.id.rowChangePassword)
        val rowNotifications = findViewById<View>(R.id.rowNotifications)
        val rowPrivacy = findViewById<View>(R.id.rowPrivacy)
        val rowAbout = findViewById<View>(R.id.rowAbout)
        val rowHistory = findViewById<View>(R.id.rowHistory)
        val rowDownloads = findViewById<View>(R.id.rowDownloads)

        val currentUser = auth.currentUser
        if (currentUser == null) { goToLogin(); return }

        tvEmail.text = currentUser.email ?: ""
        tvName.text = "Music Lover"

        // Lấy tên từ Firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Music Lover"
                tvName.text = name
                tvAvatar.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
                val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
                prefs.edit().putString("logged_name", name).putString("logged_email", currentUser.email).apply()
            }

        // Lấy số favorites thật từ Firestore
        db.collection("favorites").document(currentUser.uid).collection("songs").get()
            .addOnSuccessListener { snapshot -> tvFavCount.text = snapshot.size().toString() }
            .addOnFailureListener { tvFavCount.text = "0" }

        // Lấy lịch sử nghe thật từ Room DB
        lifecycleScope.launch(Dispatchers.IO) {
            val localDb = AppDatabase.getDatabase(this@ProfileActivity)
            val history = localDb.listenHistoryDao().getHistoryByUser(currentUser.uid)
            withContext(Dispatchers.Main) {
                tvPlaysCount.text = history.size.toString()
                val totalMins = (history.size * 3.5).toInt()
                tvHoursCount.text = "${totalMins}m"
            }
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        rowEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowHistory?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowDownloads?.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit().clear().apply()
            goToLogin()
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.profileContent).startAnimation(fadeIn)
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
        finish()
    }
}
