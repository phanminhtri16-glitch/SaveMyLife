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

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)
        val email = prefs.getString("logged_email", "") ?: ""

        val etCurrentPass = findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPass = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPass = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val progressBar = findViewById<ProgressBar>(R.id.changePassProgress)

        // Password strength indicator
        val tvStrength = findViewById<TextView>(R.id.tvPasswordStrength)
        val strengthBar = findViewById<View>(R.id.passwordStrengthBar)

        etNewPass.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s.toString()
                val strength = getPasswordStrength(pass)
                when (strength) {
                    0 -> {
                        tvStrength.text = ""; strengthBar.visibility = View.INVISIBLE
                    }

                    1 -> {
                        tvStrength.text = "Yếu"
                        tvStrength.setTextColor(android.graphics.Color.parseColor("#FF4444"))
                        strengthBar.setBackgroundColor(android.graphics.Color.parseColor("#FF4444"))
                        strengthBar.visibility = View.VISIBLE
                        strengthBar.layoutParams = strengthBar.layoutParams.also {
                            (it as android.widget.LinearLayout.LayoutParams).weight = 1f
                        }
                    }

                    2 -> {
                        tvStrength.text = "Trung bình"
                        tvStrength.setTextColor(android.graphics.Color.parseColor("#FFA500"))
                        strengthBar.setBackgroundColor(android.graphics.Color.parseColor("#FFA500"))
                        strengthBar.visibility = View.VISIBLE
                    }

                    3 -> {
                        tvStrength.text = "Mạnh ✓"
                        tvStrength.setTextColor(android.graphics.Color.parseColor("#44DD88"))
                        strengthBar.setBackgroundColor(android.graphics.Color.parseColor("#44DD88"))
                        strengthBar.visibility = View.VISIBLE
                    }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSave.setOnClickListener {
            val currentPass = etCurrentPass.text.toString()
            val newPass = etNewPass.text.toString()
            val confirmPass = etConfirmPass.text.toString()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                etConfirmPass.startAnimation(shake)
                Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@ChangePasswordActivity)
                val user = db.userDao().login(email, currentPass)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true

                    if (user == null) {
                        val shake =
                            AnimationUtils.loadAnimation(this@ChangePasswordActivity, R.anim.shake)
                        etCurrentPass.startAnimation(shake)
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Mật khẩu hiện tại không đúng",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.userDao().updatePassword(email, newPass)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Đổi mật khẩu thành công ✓",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                overridePendingTransition(
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                                )
                            }
                        }
                    }
                }
            }
        }

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        findViewById<View>(R.id.changePassCard).startAnimation(slideIn)
    }

    private fun getPasswordStrength(pass: String): Int {
        if (pass.isEmpty()) return 0
        var score = 0
        if (pass.length >= 6) score++
        if (pass.any { it.isUpperCase() } && pass.any { it.isLowerCase() }) score++
        if (pass.any { it.isDigit() } || pass.any { !it.isLetterOrDigit() }) score++
        return score
    }
}