package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)

        // ── Views ──────────────────────────────────────────
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAvatar = findViewById<TextView>(R.id.tvAvatarLetter)
        val tvStatFav = findViewById<TextView>(R.id.tvStatFav)
        val tvStatPlays = findViewById<TextView>(R.id.tvStatPlays)
        val tvStatHours = findViewById<TextView>(R.id.tvStatHours)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Menu rows
        val rowEditProfile = findViewById<LinearLayout>(R.id.rowEditProfile)
        val rowChangePass = findViewById<LinearLayout>(R.id.rowChangePassword)
        val rowNotifications = findViewById<LinearLayout>(R.id.rowNotifications)
        val rowPrivacy = findViewById<LinearLayout>(R.id.rowPrivacy)
        val rowAbout = findViewById<LinearLayout>(R.id.rowAbout)

        // ── Load user data ─────────────────────────────────
        val name = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
        val email = prefs.getString("logged_email", "") ?: ""

        tvName.text = name
        tvEmail.text = email
        tvAvatar.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"

        // Load real stats from DB
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@ProfileActivity)
            val favCount = db.userDao().getFavoriteCount(email)
            val playCount = db.userDao().getPlayCount(email)
            withContext(Dispatchers.Main) {
                tvStatFav.text = favCount.toString()
                tvStatPlays.text = playCount.toString()
                tvStatHours.text = "${(playCount * 3.5 / 60).toInt()}h"
            }
        }

        // ── Navigation ─────────────────────────────────────
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        rowEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rowChangePass.setOnClickListener {
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

        rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // ── Entrance animation ─────────────────────────────
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.profileContent).startAnimation(fadeIn)
    }

    // Refresh stats khi quay lại từ EditProfile
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val name = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
        val email = prefs.getString("logged_email", "") ?: ""
        findViewById<TextView>(R.id.tvProfileName).text = name
        findViewById<TextView>(R.id.tvProfileEmail).text = email
        findViewById<TextView>(R.id.tvAvatarLetter).text =
            name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    }
}