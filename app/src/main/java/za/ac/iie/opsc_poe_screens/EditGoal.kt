package za.ac.iie.opsc_poe_screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.opsc_poe_screens.databinding.ActivityEditGoalBinding

class EditGoal : AppCompatActivity() {

    private lateinit var binding: ActivityEditGoalBinding
    private var editingGoal: GoalEntity? = null
    private lateinit var viewModel: GoalsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        hideSystemUI()

        val dao = AppDatabase.getDatabase(this).goalDao()
        val currentUserId = UserSession.currentUserId ?: -1
        val factory = GoalsViewModelFactory(dao, currentUserId)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        val goalId = intent.getIntExtra("goalId", -1)

        // Observe all goals to manage the bonus limit and populate fields
        viewModel.allGoals.observe(this) { goals ->
            val bonusGoalsCount = goals.count { it.Bonus }

            if (goalId != -1) {
                if (editingGoal == null) {
                    editingGoal = goals.find { it.id == goalId }
                    populateFields()

                    // If this goal is already a bonus goal, the checkbox should be enabled.
                    // Otherwise, enable it only if the bonus limit has not been reached.
                    binding.cbBonus.isEnabled = editingGoal?.Bonus == true || bonusGoalsCount < 2
                }
            } else {
                // The checkbox should only be enabled if there are fewer than 2 bonus goals.
                binding.cbBonus.isEnabled = bonusGoalsCount < 2
            }

            // If the checkbox is disabled, provide feedback to the user.
            if (!binding.cbBonus.isEnabled) {
                binding.cbBonus.text = "Bonus Goal (Limit Reached)"
            } else {
                binding.cbBonus.text = "Bonus Goal"
            }
        }

        binding.cbCompleted.isEnabled = false // disables manual checking
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveGoal.setOnClickListener { saveGoal() }
    }

    private fun populateFields() {
        editingGoal?.let { goal ->
            binding.etGoalName.setText(goal.GoalName)
            binding.etDescription.setText(goal.Description)
            binding.etAmount.setText(goal.Amount.toString())
            binding.etCurrentAmount.setText(goal.CurrentAmount.toString())
            binding.cbCompleted.isChecked = goal.Completed
            binding.cbBonus.isChecked = goal.Bonus
        }
    }

    private fun saveGoal() {
        val name = binding.etGoalName.text.toString()
        val desc = binding.etDescription.text.toString()
        val amount = binding.etAmount.text.toString().toIntOrNull() ?: 0
        val current = binding.etCurrentAmount.text.toString().toIntOrNull() ?: 0
        val completed = if (amount > 0) current >= amount else false
        val bonus = binding.cbBonus.isChecked

        val userId = UserSession.currentUserId
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val goal = GoalEntity(
            id = editingGoal?.id ?: 0,
            GoalName = name,
            Description = desc,
            Amount = amount,
            CurrentAmount = current,
            Completed = completed,
            Bonus = bonus,
            userId = userId
        )

        if (editingGoal == null) {
            viewModel.addGoal(goal)
        } else {
            viewModel.updateGoal(goal)
        }

        finish()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}