package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val name = prefs.getString("logged_name", "Music Lover") ?: "Music Lover"
        val email = prefs.getString("logged_email", "") ?: ""

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvFavCount = findViewById<TextView>(R.id.tvStatFav)
        val tvPlaysCount = findViewById<TextView>(R.id.tvStatPlays)
        val tvHoursCount = findViewById<TextView>(R.id.tvStatHours)

        tvName.text = name
        tvEmail.text = email
        tvFavCount.text = "6"
        tvPlaysCount.text = "42"
        tvHoursCount.text = "12"

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnLogout.setOnClickListener {
            // Clear session
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
            finish()
        }

        // Animate entrance
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.profileContent).startAnimation(fadeIn)
    }
}