package com.example.yuxiaofy

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

class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val email = prefs.getString("logged_email", "") ?: ""

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val switchListenHist = findViewById<Switch>(R.id.switchListenHistory)
        val switchPublicProf = findViewById<Switch>(R.id.switchPublicProfile)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        // Load saved prefs
        switchListenHist.isChecked = prefs.getBoolean("privacy_listen_history", true)
        switchPublicProf.isChecked = prefs.getBoolean("privacy_public_profile", false)

        switchListenHist.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("privacy_listen_history", isChecked).apply()
        }
        switchPublicProf.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("privacy_public_profile", isChecked).apply()
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử nghe")
                .setMessage("Tất cả lịch sử nghe nhạc của bạn sẽ bị xóa. Không thể hoàn tác.")
                .setPositiveButton("Xóa") { _, _ ->
                    // TODO: clear listen history from DB when that table is added
                    Toast.makeText(this, "Đã xóa lịch sử nghe ✓", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Xóa tài khoản")
                .setMessage("Tài khoản và toàn bộ dữ liệu của bạn sẽ bị xóa vĩnh viễn. Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tài khoản") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(this@PrivacyActivity)
                        db.userDao().deleteUserByEmail(email)
                        withContext(Dispatchers.Main) {
                            prefs.edit().clear().apply()
                            Toast.makeText(
                                this@PrivacyActivity,
                                "Tài khoản đã bị xóa",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = android.content.Intent(
                                this@PrivacyActivity,
                                LoginActivity::class.java
                            ).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.privacyContent).startAnimation(fadeIn)
    }
}