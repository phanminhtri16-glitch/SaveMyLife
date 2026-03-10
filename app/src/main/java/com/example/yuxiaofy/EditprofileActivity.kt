package com.example.yuxiaofy

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)

        val tvAvatar = findViewById<TextView>(R.id.tvAvatarLetter)
        val etName = findViewById<EditText>(R.id.etEditName)
        val etEmail = findViewById<EditText>(R.id.etEditEmail)
        val btnSave = findViewById<Button>(R.id.btnSaveProfile)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val progressBar = findViewById<ProgressBar>(R.id.editProfileProgress)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Lấy thông tin từ Firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                val email = currentUser.email ?: ""
                etName.setText(name)
                etEmail.setText(email)
                tvAvatar.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            }

        // Update avatar letter live
        etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvAvatar.text = s?.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Email không cho sửa vì Firebase Auth không đổi email dễ dàng
        etEmail.isEnabled = false
        etEmail.alpha = 0.5f

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                etName.startAnimation(shake)
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false

            // Cập nhật tên lên Firestore
            db.collection("users").document(currentUser.uid)
                .update("name", newName)
                .addOnSuccessListener {
                    // Cập nhật SharedPreferences
                    prefs.edit().putString("logged_name", newName).apply()

                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Cập nhật thành công ✓", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.editProfileCard).startAnimation(slideIn)
    }
}