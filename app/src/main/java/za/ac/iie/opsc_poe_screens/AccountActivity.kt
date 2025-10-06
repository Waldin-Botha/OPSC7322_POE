package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityAccountsBinding

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var userViewModel: UserViewModel // Add ViewModel reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val decorView = getWindow().getDecorView()
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.setSystemUiVisibility(uiOptions)

        // --- ViewModel Initialization ---
        val userDao = AppDatabase.getDatabase(this).userDao()
        val factory = UserViewModelFactory(userDao)
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]

        // Load user data and display it
        loadAndDisplayUserData()

        // Set click listeners for all interactive elements
        setupClickListeners()

    }

    private fun loadAndDisplayUserData() {

        val username = UserSession.currentUsername ?: "Unknown"
        binding.tvEditableUsername.text = username
        binding.tvEditablePasswordText.text = "**********" // Mask password for security
    }

    private fun setupClickListeners() {
        // --- SIGN OUT LOGIC ---
        binding.btnProceed.setOnClickListener {
            if (binding.cbConfirmation.isChecked) {
                clearUserSession()
                val intent = Intent(this, SignIn::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Finish this activity
            } else {
                Toast.makeText(this, "Please check the confirmation box to sign out.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- DELETE ACCOUNT LOGIC ---
        binding.btnProceedDelete.setOnClickListener {
            if (binding.cbConfirm.isChecked) {
                deleteCurrentUser()
            } else {
                Toast.makeText(this, "Please check the confirmation box to delete your account.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish() // Simply finish the activity to go back
        }


        binding.btnEditPassword.setOnClickListener {
            showEditDialog("password")
        }
    }

    private fun showEditDialog(fieldToEdit: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit $fieldToEdit")

        val input = EditText(this)
        if (fieldToEdit == "password") {
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 20, 50, 20)
        }
        input.layoutParams = layoutParams
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newValue = input.text.toString().trim()
            if (newValue.isNotEmpty()) {
                updateUserValue(fieldToEdit, newValue)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "$fieldToEdit cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateUserValue(field: String, value: String) {
        val userId = UserSession.currentUserId
        if (userId == null || userId == -1) {
            Toast.makeText(this, "Error: User session not found.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val currentUser = userViewModel.getUser(userId)
            if (currentUser != null) {
                val updatedUser = if (field == "username") {
                    currentUser.copy(username = value)
                } else { // password
                    currentUser.copy(passwordHash = value) // TODO hash password
                }

                userViewModel.updateUser(updatedUser)

                // Update session and UI
                if (field == "username") {
                    UserSession.currentUsername = value
                    binding.tvEditableUsername.text = value
                } else {
                    UserSession.currentPassword = value
                }
                Toast.makeText(this@AccountActivity, "$field updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCurrentUser() {
        val userId = UserSession.currentUserId
        if (userId == null || userId == -1) {
            Toast.makeText(this, "Error: No user to delete.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val userToDelete = userViewModel.getUser(userId)
            if (userToDelete != null) {
                userViewModel.deleteUser(userToDelete)
                Toast.makeText(this@AccountActivity, "Account deleted successfully.", Toast.LENGTH_SHORT).show()

                // Sign out and navigate to Welcome screen
                clearUserSession()
                val intent = Intent(this@AccountActivity, WelcomeScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@AccountActivity, "Error: Could not find user to delete.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearUserSession() {
        // Clear in-memory session
        UserSession.currentUserId = -1
        UserSession.currentUsername = null
        UserSession.currentPassword = null

        // Clear SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this, "You have been signed out.", Toast.LENGTH_SHORT).show()
    }
}