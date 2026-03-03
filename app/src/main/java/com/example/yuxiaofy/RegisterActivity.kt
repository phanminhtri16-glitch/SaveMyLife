package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        // 1. Ánh xạ các ô nhập liệu từ layout activity_register.xml
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm tra nhập liệu cơ bản
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Sử dụng lifecycleScope để thực hiện tác vụ bất đồng bộ [cite: 23, 89]
            lifecycleScope.launch(Dispatchers.IO) { // Chạy trên luồng I/O theo khuyến nghị [cite: 23, 189]
                try {
                    val db = AppDatabase.getDatabase(this@RegisterActivity)
                    val newUser = User(name = name, email = email, password = password)

                    // Gọi hàm DAO để lưu user vào database
                    db.userDao().registerUser(newUser)

                    // 3. Quay lại luồng chính (Main Thread) để hiển thị thông báo và chuyển màn hình [cite: 176]
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Đăng ký thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
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
        }
    }
}