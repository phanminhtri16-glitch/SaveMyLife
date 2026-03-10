package com.example.yuxiaofy

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Switch states - load from prefs
        val switchNewReleases = findViewById<Switch>(R.id.switchNewReleases)
        val switchRecommended = findViewById<Switch>(R.id.switchRecommended)
        val switchPlaylistUpdate = findViewById<Switch>(R.id.switchPlaylistUpdate)
        val switchWeeklyMix = findViewById<Switch>(R.id.switchWeeklyMix)
        val switchPromo = findViewById<Switch>(R.id.switchPromo)

        // Load saved preferences
        switchNewReleases.isChecked = prefs.getBoolean("notif_new_releases", true)
        switchRecommended.isChecked = prefs.getBoolean("notif_recommended", true)
        switchPlaylistUpdate.isChecked = prefs.getBoolean("notif_playlist", false)
        switchWeeklyMix.isChecked = prefs.getBoolean("notif_weekly", true)
        switchPromo.isChecked = prefs.getBoolean("notif_promo", false)

        // Save on toggle
        switchNewReleases.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_new_releases", isChecked).apply()
            showToast(if (isChecked) "Bật thông báo bài mới" else "Tắt thông báo bài mới")
        }
        switchRecommended.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_recommended", isChecked).apply()
        }
        switchPlaylistUpdate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_playlist", isChecked).apply()
        }
        switchWeeklyMix.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_weekly", isChecked).apply()
        }
        switchPromo.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_promo", isChecked).apply()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.notifContent).startAnimation(fadeIn)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}