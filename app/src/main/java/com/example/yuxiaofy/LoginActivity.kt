package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Nếu đã đăng nhập rồi thì vào Home luôn
        if (auth.currentUser != null) {
            goToHome()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Đang đăng nhập..."

            // Đăng nhập bằng Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Lấy thông tin user từ Firestore
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("name") ?: "bạn"
                            Toast.makeText(this, "Chào mừng, $name!", Toast.LENGTH_SHORT).show()
                            goToHome(name)
                        }
                        .addOnFailureListener {
                            // Vẫn vào Home dù không lấy được tên
                            goToHome()
                        }
                }
                .addOnFailureListener { e ->
                    val errorMsg = when {
                        e.message?.contains("no user record") == true -> "Email chưa được đăng ký!"
                        e.message?.contains("password is invalid") == true -> "Mật khẩu không đúng!"
                        e.message?.contains("badly formatted") == true -> "Email không hợp lệ!"
                        else -> "Đăng nhập thất bại: ${e.message}"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Đăng nhập"
                }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToHome(name: String = "") {
        val intent = Intent(this, HomeActivity::class.java)
        if (name.isNotEmpty()) intent.putExtra("USER_NAME", name)
        startActivity(intent)
        finish()
    }
}