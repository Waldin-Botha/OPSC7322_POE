package za.ac.iie.opsc_poe_screens

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.opsc_poe_screens.databinding.ActivityAchievementsBinding

class Achievements : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var goalsViewModel: GoalsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()

        // --- ViewModel Initialization ---
        val goalDao = AppDatabase.getDatabase(this).goalDao()
        val currentUserId = UserSession.currentUserId ?: -1
        val factory = GoalsViewModelFactory(goalDao, currentUserId)
        goalsViewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        // --- Display Daily Streak ---
        val currentStreak = StreakManager.getStreak(this)
        binding.tvDaysNumber.text = currentStreak.toString()

        // --- Observe All Goal-Related Data ---
        observeGoalsAndUpdateUI()

        // --- Back Button Listener ---
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun observeGoalsAndUpdateUI() {
        goalsViewModel.allGoals.observe(this) { goals ->
            // --- 1. Filter goals by their status ---
            val completedGoals = goals.filter { it.Completed }
            val completedBonusGoals = completedGoals.filter { it.Bonus }

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
                totalPoints >= 300 -> { // Gold tier at 300 points
                    binding.tvStatusDescription.text = "Gold"
                    // Optionally, change the trophy image
                    binding.imgBronze.setImageResource(R.drawable.ic_trophy_foreground) // Make sure you have this drawable
                }
                totalPoints >= 200 -> { // Silver tier at 200 points
                    binding.tvStatusDescription.text = "Silver"
                    binding.imgBronze.setImageResource(R.drawable.ic_trophy_foreground) // Make sure you have this drawable
                }
                else -> {
                    binding.tvStatusDescription.text = "Bronze"
                    binding.imgBronze.setImageResource(R.drawable.ic_reward_foreground) // Your default bronze trophy
                }
            }

            // --- 5. Display Bonus Goal Names ---
            val allBonusGoals = goals.filter { it.Bonus }

            binding.tvAccount1.text = if (allBonusGoals.isNotEmpty()) allBonusGoals[0].GoalName else "Bonus Goal 1"
            binding.tvAccount1Points.text = "+50" // Display potential points
            binding.cAccount1.visibility = if (allBonusGoals.isNotEmpty()) View.VISIBLE else View.GONE


            binding.tvAccount2.text = if (allBonusGoals.size > 1) allBonusGoals[1].GoalName else "Bonus Goal 2"
            binding.tvAccount2Points.text = "+50" // Display potential points
            binding.cAccount2.visibility = if (allBonusGoals.size > 1) View.VISIBLE else View.GONE
        }
    }

    private fun hideSystemUI() {
        // Hide the default ActionBar
        supportActionBar?.hide()

        //hide UI elements
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}