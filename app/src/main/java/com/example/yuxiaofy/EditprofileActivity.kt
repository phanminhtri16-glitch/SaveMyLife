package com.example.yuxiaofy

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val name = prefs.getString("logged_name", "") ?: ""
        val email = prefs.getString("logged_email", "") ?: ""

        val tvAvatar = findViewById<TextView>(R.id.tvAvatarLetter)
        val etName = findViewById<EditText>(R.id.etEditName)
        val etEmail = findViewById<EditText>(R.id.etEditEmail)
        val btnSave = findViewById<Button>(R.id.btnSaveProfile)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val progressBar = findViewById<ProgressBar>(R.id.editProfileProgress)

        // Pre-fill data
        etName.setText(name)
        etEmail.setText(email)
        tvAvatar.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"

        // Update avatar letter live
        etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvAvatar.text = s?.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                if (newName.isEmpty()) etName.startAnimation(shake)
                if (newEmail.isEmpty()) etEmail.startAnimation(shake)
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@EditProfileActivity)

                // Check if new email is taken by someone else
                val existing = db.userDao().getUserByEmail(newEmail)
                if (existing != null && existing.email != email) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Email đã được sử dụng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Update DB
                db.userDao().updateUser(email, newName, newEmail)

                // Update SharedPreferences
                withContext(Dispatchers.Main) {
                    prefs.edit()
                        .putString("logged_name", newName)
                        .putString("logged_email", newEmail)
                        .apply()

                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Cập nhật thành công ✓",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.editProfileCard).startAnimation(slideIn)
    }
}