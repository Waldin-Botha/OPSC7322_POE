package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivitySignUpBinding // Using View Binding
import kotlin.math.log

class SignUp : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var binding: ActivitySignUpBinding // View Binding instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        hideSystemUI()

        repository = FirebaseRepository()

        binding.btnCreateAccount.setOnClickListener {
            val username = binding.etUsername.text.toString().trim() // This is now an email
            val password = binding.etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if ("@" in username) {
                Toast.makeText(this, "Username cannot contain the '@' symbol", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnCreateAccount.isEnabled = false

            lifecycleScope.launch {
                try {
                    val username = binding.etUsername.text.toString().trim()
                    val password = binding.etPassword.text.toString()

                    // --- Your existing validation ---
                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this@SignUp, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                        binding.btnCreateAccount.isEnabled = true
                        return@launch
                    }
                    if (password.length < 6) {
                        Toast.makeText(this@SignUp, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                        binding.btnCreateAccount.isEnabled = true
                        return@launch
                    }
                    if ("@" in username) {
                        Toast.makeText(this@SignUp, "Username cannot contain the '@' symbol", Toast.LENGTH_SHORT).show()
                        binding.btnCreateAccount.isEnabled = true
                        return@launch
                    }
                    // ---------------------------------

                    // 1. Create the user in the 'users' collection and get their new ID
                    val newUserId = repository.manualSignUp(username, password)

                    // 2. Create all default data using this new ID
                    repository.createDefaultUserData(newUserId)

                    // 3. Set the user ID in our session object
                    UserSession.currentUserId = newUserId
                    UserSession.currentUsername = username

                    // 4. Update streak
                    StreakManager.updateUserStreak(this@SignUp)

                    // 5. Success: Go to MainActivity directly. No need to sign in again.
                    Toast.makeText(this@SignUp, "Account created successfully!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignUp, MainActivity::class.java))
                    finish() // Finish so the user can't go back

                } catch (e: Exception) {
                    binding.btnCreateAccount.isEnabled = true
                    // Display the error (e.g., "Username is already taken.")
                    Toast.makeText(this@SignUp, "Sign-up failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("Signup", "Sign-up failed: ${e.message}", e)
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