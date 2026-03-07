package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgress)

        // Entrance animation
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.loginCard).startAnimation(slideUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                if (email.isEmpty()) etEmail.startAnimation(shake)
                if (password.isEmpty()) etPassword.startAnimation(shake)
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@LoginActivity)
                val user = db.userDao().login(email, password)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    if (user != null) {
                        // Lưu session
                        getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("logged_email", user.email)
                            .putString("logged_name", user.name)
                            .apply()

                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Email hoặc mật khẩu không đúng",
                            Toast.LENGTH_SHORT
                        ).show()
                        val shake = AnimationUtils.loadAnimation(this@LoginActivity, R.anim.shake)
                        etPassword.startAnimation(shake)
                    }
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}