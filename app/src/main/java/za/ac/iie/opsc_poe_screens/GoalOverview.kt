package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.opsc_poe_screens.databinding.ActivityGoalOverviewBinding

/**
 * Activity displaying an overview of all goals.
 * Shows a RecyclerView of goals, a summary of total and completed goals,
 * and allows navigation to add or edit a goal.
 */
class GoalOverview : AppCompatActivity() {

    private lateinit var binding: ActivityGoalOverviewBinding
    private lateinit var adapter: GoalAdapter
    private lateinit var viewModel: GoalsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Hide top and bottom UI
        hideSystemUI()

        // Initialize ViewModel with the DAO and current user ID
        val dao = AppDatabase.getDatabase(this).goalDao()
        val currentUserId = UserSession.currentUserId ?: -1 // Get the current user ID
        val factory = GoalsViewModelFactory(dao, currentUserId)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        setupRecyclerView()
        updateSummary() // initial summary with empty list

        // Setup button listeners
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAddGoal.setOnClickListener {
            val intent = Intent(this, EditGoal::class.java)
            startActivity(intent)
        }

        // Observe LiveData for changes in goals
        viewModel.allGoals.observe(this) { goals ->
            adapter.updateGoals(goals)
            updateSummary(goals)
        }
    }

    /**
     * Configures the RecyclerView for displaying goals.
     * Attaches the GoalAdapter with edit and delete callbacks.
     */
    private fun setupRecyclerView() {
        adapter = GoalAdapter(
            mutableListOf(),
            onEdit = { goal ->
                // Launch EditGoal activity with the selected goal's ID
                val intent = Intent(this, EditGoal::class.java)
                intent.putExtra("goalId", goal.id)
                startActivity(intent)
            },
            onDelete = { goal ->
                // Delete the selected goal via ViewModel
                viewModel.deleteGoal(goal)
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(this)
        binding.rvGoals.adapter = adapter
    }

    /**
     * Updates the summary section showing total and completed goals.
     * @param goals The current list of goals to summarize.
     */
    private fun updateSummary(goals: List<GoalEntity> = listOf()) {
        val totalGoals = goals.size
        val completedGoals = goals.count { it.Completed }

        binding.tvTotalGoals.text = totalGoals.toString()
        binding.tvCompletedGoals.text = completedGoals.toString()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}