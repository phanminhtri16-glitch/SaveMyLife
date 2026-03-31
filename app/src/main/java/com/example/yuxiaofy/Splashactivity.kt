package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Đã đăng nhập → check role
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val isAdmin = doc.getBoolean("isAdmin") ?: false
                        val name = doc.getString("name") ?: ""
                        getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE).edit()
                            .putString("logged_name", name)
                            .putString("logged_email", currentUser.email ?: "")
                            .putBoolean("is_admin", isAdmin)
                            .apply()
                        val target = if (isAdmin) AdminActivity::class.java else HomeActivity::class.java
                        startActivity(Intent(this, target))
                        finish()
                    }
                    .addOnFailureListener {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
            } else {
                // Chưa đăng nhập → Login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }, 2000)
    }
}