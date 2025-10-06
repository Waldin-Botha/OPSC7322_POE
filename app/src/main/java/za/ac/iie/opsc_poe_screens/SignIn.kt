package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignIn : AppCompatActivity() {
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        supportActionBar?.hide()
        hideSystemUI()

        val db = AppDatabase.getDatabase(this)
        userDao = db.userDao()

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = userDao.login(username, password)
                if (user != null) {
                    // --- START OF STREAK LOGIC ---
                    // Check last login date and update streak accordingly.
                    StreakManager.updateUserStreak(this@SignIn)
                    // --- END OF STREAK LOGIC ---

                    // Save globally to UserSession
                    UserSession.currentUserId = user.id
                    UserSession.currentUsername = user.username
                    UserSession.currentPassword = user.passwordHash

                    // Save to SharedPreferences for persistence
                    val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("userId", user.id) // Also save the ID
                    editor.putString("username", user.username)
                    editor.putString("password", user.passwordHash)
                    editor.apply()

                    // Navigate to MainActivity after successful login
                    val intent = Intent(this@SignIn, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this@SignIn, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}