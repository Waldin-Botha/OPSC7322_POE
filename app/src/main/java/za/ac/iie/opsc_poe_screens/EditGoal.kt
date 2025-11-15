package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityEditGoalBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditGoal : AppCompatActivity() {

    private lateinit var binding: ActivityEditGoalBinding
    private lateinit var viewModel: GoalsViewModel
    private lateinit var repository: FirebaseRepository
    private lateinit var currentUserId: String

    private var editingGoal: Goal? = null
    private var goalId: String? = null

    // To hold the list of user's accounts for the spinner
    private var accountsList: List<Account> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        hideSystemUI()

        val userId = UserSession.currentUserId
        if (userId == null) {
            Toast.makeText(this, "Session expired. Please sign in.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SignIn::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        currentUserId = userId

        repository = FirebaseRepository()
        val factory = GoalsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        goalId = intent.getStringExtra("goalId")

        // Load all necessary data (accounts and goals)
        loadInitialData()

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveGoal.setOnClickListener { saveGoal() }

        // Set the title based on whether we are creating or editing
        if (goalId == null) {
            binding.tvEditGoalTitle.text = "Create New Goal"
        } else {
            binding.tvEditGoalTitle.text = "Edit Goal"
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                // Fetch accounts and goals in parallel
                accountsList = repository.getUserAccounts(currentUserId)
                val allGoals = repository.getAllGoals(currentUserId)

                // Count how many goals are marked as bonus, excluding the one we might be currently editing.
                val existingBonusGoalsCount = allGoals.count { it.bonus && it.id != goalId }
                updateBonusCheckboxState(existingBonusGoalsCount)

                // Populate the accounts spinner
                setupAccountSpinner()

                // If we are editing, find the specific goal and populate fields
                if (goalId != null) {
                    editingGoal = allGoals.find { it.id == goalId }
                    populateFields()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditGoal, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAccountSpinner() {
        val accountNames = accountsList.map { it.accountName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerAccount.adapter = adapter
    }

    private fun populateFields() {
        editingGoal?.let { goal ->
            binding.etGoalName.setText(goal.goalName)
            binding.etAmount.setText(goal.amount.toString())

            binding.cbBonus.isChecked = goal.bonus

            // Set account spinner selection
            val accountIndex = accountsList.indexOfFirst { it.id == goal.accountId }
            if (accountIndex != -1) {
                binding.spinnerAccount.setSelection(accountIndex)
            }

            // Set goal type radio button
            if (goal.goalType == GoalType.SAVINGS.name) {
                binding.rbSavings.isChecked = true
            } else {
                binding.rbSpending.isChecked = true
            }

            // When editing, the user cannot change the goal type or linked account.
            binding.spinnerAccount.isEnabled = false
            binding.rbSavings.isEnabled = false
            binding.rbSpending.isEnabled = false
        }
    }

    private fun saveGoal() {
        // --- Input Validation ---
        val name = binding.etGoalName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Goal name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Target amount must be a positive number.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAccount = accountsList.getOrNull(binding.spinnerAccount.selectedItemPosition)
        if (selectedAccount == null) {
            Toast.makeText(this, "Please select an account to link this goal to.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Create Goal Object ---
        val goalType = if (binding.rbSavings.isChecked) GoalType.SAVINGS.name else GoalType.SPENDING.name

        // Format the current month-year string (e.g., "2025-11")
        val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = monthYearFormat.format(Date())

        val goalToSave = Goal(
            id = editingGoal?.id ?: "", // Use existing ID for update, or empty for new
            goalName = name,
            description = editingGoal?.description ?: "", // Keep old description or set empty
            amount = amount,
            accountId = selectedAccount.id,
            goalType = goalType,
            monthYear = editingGoal?.monthYear ?: currentMonthYear, // Keep original month or set new
            currentAmount = editingGoal?.currentAmount ?: 0.0, // Preserve current progress
            bonus = binding.cbBonus.isChecked
        )

        // --- Save via ViewModel ---
        if (editingGoal == null) {
            viewModel.addGoal(currentUserId, goalToSave)
            Toast.makeText(this, "Goal created!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.updateGoal(currentUserId, goalToSave)
            Toast.makeText(this, "Goal updated!", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun updateBonusCheckboxState(bonusGoalCount: Int) {
        val MAX_BONUS_GOALS = 2

        // Check if we are currently editing a goal that is already a bonus goal.
        val isEditingCurrentBonusGoal = editingGoal?.bonus ?: false

        if (isEditingCurrentBonusGoal) {
            // If we are editing an existing bonus goal, the checkbox should be enabled and checked.
            binding.cbBonus.isEnabled = true
            binding.cbBonus.text = "Bonus Goal"
        } else if (bonusGoalCount >= MAX_BONUS_GOALS) {
            // If the max limit is reached and we are not editing one of them, disable the checkbox.
            binding.cbBonus.isEnabled = false
            binding.cbBonus.text = "Bonus Goal (Limit Reached)"
            binding.cbBonus.isChecked = false // Ensure it's not checked
        } else {
            // Otherwise, the user is free to make this a bonus goal.
            binding.cbBonus.isEnabled = true
            binding.cbBonus.text = "Bonus Goal"
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