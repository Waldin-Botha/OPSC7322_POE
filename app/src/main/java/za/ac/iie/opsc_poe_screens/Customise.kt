package za.ac.iie.opsc_poe_screens

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Customise : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportActionBar?.hide()

        val decorView = getWindow().getDecorView()
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.setSystemUiVisibility(uiOptions)

        setContentView(R.layout.activity_customise)

        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

    }
}