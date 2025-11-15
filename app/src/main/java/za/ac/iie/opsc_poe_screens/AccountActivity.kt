package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityAccountsBinding

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var repository: FirebaseRepository // Use our repository
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize our repository
        repository = FirebaseRepository()

        hideSystemUI()

        // Load user data and set up listeners
        loadAndDisplayUserData()
        setupClickListeners()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }

    private fun loadAndDisplayUserData() {
        // Get username and ID from our UserSession singleton
        val username = UserSession.currentUsername
        currentUserId = UserSession.currentUserId

        if (currentUserId != null && username != null) {
            // Display the username from the session
            binding.tvEditableUsername.text = username
            binding.tvEditablePasswordText.text = "**********" // Keep password masked
        } else {
            // User is not logged in, redirect them.
            Toast.makeText(this, "User session not found. Please sign in.", Toast.LENGTH_LONG).show()
            goToSignIn()
        }
    }

    private fun setupClickListeners() {
        // --- SIGN OUT LOGIC ---
        binding.btnProceed.setOnClickListener {
            if (binding.cbConfirmation.isChecked) {
                signOutAndGoToSignIn()
            } else {
                Toast.makeText(this, "Please check the confirmation box to sign out.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- DELETE ACCOUNT LOGIC ---
        binding.btnProceedDelete.setOnClickListener {
            if (binding.cbConfirm.isChecked) {
                confirmAccountDeletion()
            } else {
                Toast.makeText(this, "Please check the confirmation box to delete your account.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish() // Simply finish the activity to go back
        }

        binding.btnEditPassword.setOnClickListener {
            showEditPasswordDialog()
        }
    }

    private fun showEditPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Password")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter new password"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(50, 20, 50, 20) }
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newPassword = input.text.toString().trim()
            if (newPassword.length >= 6) {
                // Call our new manual password update method
                updateUserPassword(newPassword)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateUserPassword(newPassword: String) {
        val userId = currentUserId
        if (userId == null) {
            Toast.makeText(this, "Cannot update password: User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                repository.updateUserPassword(userId, newPassword)
                Toast.makeText(this@AccountActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AccountActivity, "Failed to update password: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAccountDeletion() {
        AlertDialog.Builder(this)
            .setTitle("DELETE ACCOUNT")
            .setMessage("This action is permanent and cannot be undone. All your data will be lost. Are you absolutely sure?")
            .setPositiveButton("DELETE") { _, _ -> deleteCurrentUser() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun deleteCurrentUser() {
        val userId = currentUserId
        if (userId == null) {
            Toast.makeText(this, "Cannot delete account: User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                repository.deleteUserAccount(userId)
                // Clear the session after successful deletion
                UserSession.currentUserId = null
                UserSession.currentUsername = null

                Toast.makeText(this@AccountActivity, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                // Navigate to Welcome screen after deletion
                val intent = Intent(this@AccountActivity, WelcomeScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AccountActivity, "Could not delete account: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signOutAndGoToSignIn() {
        // Clear the user session manually
        UserSession.currentUserId = null
        UserSession.currentUsername = null

        goToSignIn()
    }

    private fun goToSignIn(){
        Toast.makeText(this, "You have been signed out.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}