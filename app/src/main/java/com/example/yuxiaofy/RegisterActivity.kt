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
import database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)
        val progressBar = findViewById<ProgressBar>(R.id.registerProgress)

        // Entrance animation
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.registerCard).startAnimation(slideUp)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                etConfirmPassword.startAnimation(shake)
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@RegisterActivity)
                    val existing = db.userDao().getUserByEmail(email)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true

                        if (existing != null) {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Email đã được sử dụng",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.userDao().registerUser(
                                    User(
                                        name = name,
                                        email = email,
                                        password = password
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "Đăng ký thành công! 🎵",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(
                                        Intent(
                                            this@RegisterActivity,
                                            LoginActivity::class.java
                                        )
                                    )
                                    overridePendingTransition(
                                        R.anim.slide_in_left,
                                        R.anim.slide_out_right
                                    )
                                    finish()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(
                            this@RegisterActivity,
                            "Lỗi: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}