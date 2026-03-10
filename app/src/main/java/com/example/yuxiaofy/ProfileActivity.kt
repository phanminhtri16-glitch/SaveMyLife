package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvFavCount = findViewById<TextView>(R.id.tvStatFav)
        val tvPlaysCount = findViewById<TextView>(R.id.tvStatPlays)
        val tvHoursCount = findViewById<TextView>(R.id.tvStatHours)

        // Lấy thông tin user từ Firebase Auth trước
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Chưa đăng nhập → về Login
            goToLogin()
            return
        }

        // Hiển thị email từ Firebase Auth
        tvEmail.text = currentUser.email ?: ""
        tvName.text = "Music Lover"

        // Lấy tên từ Firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Music Lover"
                tvName.text = name

                // Lưu vào SharedPreferences để dùng ở HomeActivity
                val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
                prefs.edit().putString("logged_name", name).apply()
                prefs.edit().putString("logged_email", currentUser.email).apply()
            }

        tvFavCount.text = "6"
        tvPlaysCount.text = "42"
        tvHoursCount.text = "12"

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnLogout.setOnClickListener {
            // Đăng xuất Firebase Auth
            auth.signOut()

            // Xóa SharedPreferences
            val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            // Về màn hình Login
            goToLogin()
        }

        // Animate entrance
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.profileContent).startAnimation(fadeIn)
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
        finish()
    }
}