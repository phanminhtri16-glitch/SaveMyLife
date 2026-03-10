package com.example.yuxiaofy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val rowRateApp = findViewById<LinearLayout>(R.id.rowRateApp)
        val rowShareApp = findViewById<LinearLayout>(R.id.rowShareApp)
        val rowFeedback = findViewById<LinearLayout>(R.id.rowFeedback)
        val rowTerms = findViewById<LinearLayout>(R.id.rowTerms)

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        rowRateApp.setOnClickListener {
            // Open Play Store
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${packageName}")
                    )
                )
            } catch (e: Exception) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                    )
                )
            }
        }

        rowShareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Yuxiaofy — Music App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Nghe nhạc cùng Yuxiaofy! Tải ngay: https://play.google.com/store/apps/details?id=${packageName}"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ ứng dụng"))
        }

        rowFeedback.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@yuxiaofy.com")
                putExtra(Intent.EXTRA_SUBJECT, "Phản hồi về Yuxiaofy")
            }
            startActivity(Intent.createChooser(emailIntent, "Gửi phản hồi"))
        }

        rowTerms.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://yuxiaofy.com/terms")
                )
            )
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        findViewById<View>(R.id.aboutContent).startAnimation(fadeIn)
    }
}