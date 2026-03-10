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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Nếu đã đăng nhập rồi thì check phân quyền luôn
        if (auth.currentUser != null) {
            checkRoleAndNavigate(auth.currentUser!!.uid)
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

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener
                    checkRoleAndNavigate(userId)
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

    private fun checkRoleAndNavigate(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "bạn"
                val isAdmin = doc.getBoolean("isAdmin") ?: false

                // Lưu vào SharedPreferences
                val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("logged_name", name)
                    .putString("logged_email", doc.getString("email") ?: "")
                    .putBoolean("is_admin", isAdmin)
                    .apply()

                Toast.makeText(this, "Chào mừng, $name!", Toast.LENGTH_SHORT).show()

                if (isAdmin) {
                    // Admin → vào AdminActivity
                    startActivity(Intent(this, AdminActivity::class.java).apply {
                        putExtra("USER_NAME", name)
                    })
                } else {
                    // User thường → vào HomeActivity
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        putExtra("USER_NAME", name)
                    })
                }
                finish()
            }
            .addOnFailureListener {
                // Không lấy được role → vào Home mặc định
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
    }
}