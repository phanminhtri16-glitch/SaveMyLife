package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nếu bạn có file layout là activity_splash, hãy dùng dòng dưới
        // setContentView(R.layout.activity_splash)

        // Đợi 2 giây rồi chuyển sang HomeActivity hoặc LoginActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}