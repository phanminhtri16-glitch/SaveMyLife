package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm tra nhập liệu
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Đang đăng ký..."

            // Đăng ký bằng Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Lưu thông tin user lên Firestore
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "isAdmin" to false,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đăng ký thành công! Chào ${name}!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi lưu thông tin: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnRegister.isEnabled = true
                            btnRegister.text = "Đăng ký"
                        }
                }
                .addOnFailureListener { e ->
                    val errorMsg = when {
                        e.message?.contains("email address is already") == true -> "Email này đã được đăng ký!"
                        e.message?.contains("badly formatted") == true -> "Email không hợp lệ!"
                        else -> "Đăng ký thất bại: ${e.message}"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Đăng ký"
                }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}