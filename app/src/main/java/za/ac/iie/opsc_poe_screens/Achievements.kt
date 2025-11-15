package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.opsc_poe_screens.databinding.ActivityAchievementsBinding

class Achievements : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var goalsViewModel: GoalsViewModel
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()

        val userId = UserSession.currentUserId

        // Check if the userId is null (user is not logged in).
        if (userId == null) {
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()

            // Redirect to SignIn and clear the back stack so the user can't come back here
            val intent = Intent(this, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish() // Close this activity
            return   // Stop executing further code in this method
        }

        // If we reach here, userId is valid. Store it in the class variable.
        currentUserId = userId

        // Initialize ViewModel with FirebaseRepository
        val repository = FirebaseRepository()
        val factory = GoalsViewModelFactory(repository)
        goalsViewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        // --- Display Daily Streak ---
        // StreakManager is a local utility, so it can remain as is.
        val currentStreak = StreakManager.getStreak(this)
        binding.tvDaysNumber.text = currentStreak.toString()

        // --- Observe Goal Data and Errors ---
        observeGoalsAndUpdateUI()
        observeErrors()

        // --- Back Button Listener ---
        binding.btnBack.setOnClickListener {
            finish()
        }

        // --- Trigger initial data load ---
        goalsViewModel.loadAllGoals(currentUserId)
    }

    private fun observeGoalsAndUpdateUI() {
        goalsViewModel.allGoals.observe(this) { goals ->
            // --- 1. Filter goals by their status ---
            // Using the new 'Goal' data class properties
            val completedGoals = goals.filter { it.completed }
            val completedBonusGoals = completedGoals.filter { it.bonus }

            // --- 2. Update simple counts ---
            binding.tvGoalsNumber.text = completedGoals.size.toString()
            binding.tvAchievedGoalsNumbers.text = completedGoals.size.toString()

            // --- 3. Calculate Total Points ---
            val standardGoalPoints = completedGoals.size * 25
            val bonusGoalExtraPoints = completedBonusGoals.size * 50
            val totalPoints = standardGoalPoints + bonusGoalExtraPoints
            binding.tvPointsNumber.text = totalPoints.toString()

            // --- 4. Update Tier Status (Bronze, Silver, Gold) ---
            when {
                totalPoints >= 300 -> {
                    binding.tvStatusDescription.text = "Gold"
                    binding.imgBronze.setImageResource(R.drawable.ic_trophy_foreground)
                }
                totalPoints >= 200 -> {
                    binding.tvStatusDescription.text = "Silver"
                    binding.imgBronze.setImageResource(R.drawable.ic_trophy_foreground)
                }
                else -> {
                    binding.tvStatusDescription.text = "Bronze"
                    binding.imgBronze.setImageResource(R.drawable.ic_reward_foreground)
                }
            }

            // --- 5. Display Bonus Goal Names ---
            val allBonusGoals = goals.filter { it.bonus }

            binding.tvAccount1.text = if (allBonusGoals.isNotEmpty()) allBonusGoals[0].goalName else "Bonus Goal 1"
            binding.tvAccount1Points.text = "+50"
            binding.cAccount1.visibility = if (allBonusGoals.isNotEmpty()) View.VISIBLE else View.GONE

            binding.tvAccount2.text = if (allBonusGoals.size > 1) allBonusGoals[1].goalName else "Bonus Goal 2"
            binding.tvAccount2Points.text = "+50"
            binding.cAccount2.visibility = if (allBonusGoals.size > 1) View.VISIBLE else View.GONE
        }
    }

    private fun observeErrors() {
        goalsViewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideSystemUI() {
        supportActionBar?.hide()
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}