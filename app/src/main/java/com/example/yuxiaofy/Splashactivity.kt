package com.example.yuxiaofy

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivSplashLogo)
        val appName = findViewById<TextView>(R.id.tvSplashName)
        val tagline = findViewById<TextView>(R.id.tvSplashTagline)

        // Animate logo pulse
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
        logo.startAnimation(pulseAnim)

        // Fade in text
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        appName.startAnimation(fadeIn)
        tagline.startAnimation(fadeIn)

        lifecycleScope.launch {
            delay(3500)

            // Check saved session
            val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
            val savedEmail = prefs.getString("logged_email", null)

            if (savedEmail != null) {
                // Session còn -> vào thẳng Home
                startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}