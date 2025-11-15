package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivitySignInBinding // Using View Binding

class SignIn : AppCompatActivity() {

    // --- Firebase and Repository ---
    private lateinit var repository: FirebaseRepository
    private lateinit var binding: ActivitySignInBinding // Switched to View Binding for safety

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        hideSystemUI()

        repository = FirebaseRepository()

        // Handle the login button click
        binding.btnLogin.setOnClickListener {
            // Get username and password from the input fields
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Basic validation
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false

            // Launch a coroutine to handle the sign-in process
            lifecycleScope.launch {
                try {
                    val username = binding.etUsername.text.toString().trim()
                    val password = binding.etPassword.text.toString()

                    // Call the new manual sign-in method from the repository
                    val userId = repository.manualSignIn(username, password)

                    // Set the current user's ID and username in the session
                    UserSession.currentUserId = userId
                    UserSession.currentUsername = username

                    // --- Your existing logic continues here ---
                    StreakManager.updateUserStreak(this@SignIn)

                    // Navigate to MainActivity after successful login
                    Toast.makeText(this@SignIn, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignIn, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Finish SignIn activity so user can't go back to it

                } catch (e: Exception) {
                    binding.btnLogin.isEnabled = true
                    // Display the specific error from our manual sign-in logic
                    Toast.makeText(this@SignIn, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
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