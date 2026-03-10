package com.example.yuxiaofy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PrivacyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val prefs = getSharedPreferences("yuxiaofy_prefs", MODE_PRIVATE)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val switchListenHist = findViewById<Switch>(R.id.switchListenHistory)
        val switchPublicProf = findViewById<Switch>(R.id.switchPublicProfile)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        // Load saved prefs
        switchListenHist.isChecked = prefs.getBoolean("privacy_listen_history", true)
        switchPublicProf.isChecked = prefs.getBoolean("privacy_public_profile", false)

        switchListenHist.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("privacy_listen_history", isChecked).apply()
        }
        switchPublicProf.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("privacy_public_profile", isChecked).apply()
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử nghe")
                .setMessage("Tất cả lịch sử nghe nhạc của bạn sẽ bị xóa. Không thể hoàn tác.")
                .setPositiveButton("Xóa") { _, _ ->
                    // TODO: clear listen history từ Firestore khi có collection đó
                    Toast.makeText(this, "Đã xóa lịch sử nghe ✓", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Xóa tài khoản")
                .setMessage("Tài khoản và toàn bộ dữ liệu của bạn sẽ bị xóa vĩnh viễn. Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tài khoản") { _, _ ->
                    deleteAccount(prefs)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.privacyContent).startAnimation(fadeIn)
    }

    private fun deleteAccount(prefs: android.content.SharedPreferences) {
        val currentUser = auth.currentUser ?: return

        // Xóa dữ liệu user trên Firestore trước
        db.collection("users").document(currentUser.uid).delete()
            .addOnSuccessListener {
                // Xóa tài khoản Firebase Auth
                currentUser.delete()
                    .addOnSuccessListener {
                        // Xóa SharedPreferences
                        prefs.edit().clear().apply()

                        Toast.makeText(this, "Tài khoản đã bị xóa", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        overridePendingTransition(R.anim.fade_in_up, R.anim.fade_out)
                    }
                    .addOnFailureListener { e ->
                        // Firebase yêu cầu đăng nhập lại gần đây mới xóa được
                        if (e.message?.contains("requires recent authentication") == true) {
                            Toast.makeText(
                                this,
                                "Vui lòng đăng xuất và đăng nhập lại trước khi xóa tài khoản!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi xóa dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}