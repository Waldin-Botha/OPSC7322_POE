package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.opsc_poe_screens.databinding.ActivityGoalOverviewBinding

/**
 * Activity displaying an overview of all goals from Firebase.
 * Shows a RecyclerView of goals, a summary, and allows navigation to add/edit.
 */
class GoalOverview : AppCompatActivity() {

    private lateinit var binding: ActivityGoalOverviewBinding
    private lateinit var adapter: GoalAdapter
    private lateinit var viewModel: GoalsViewModel
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        hideSystemUI()

        // Get the user ID from our manual UserSession singleton
        val userId = UserSession.currentUserId

        // Check if the user is actually logged in
        if (userId == null) {
            // User is not logged in. Redirect them and stop loading this screen.
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish() // Close this activity
            return   // Stop executing any further code in onCreate
        }

        // If we reach here, the user is logged in. Store their ID.
        currentUserId = userId


        // Initialize ViewModel with FirebaseRepository
        val repository = FirebaseRepository()
        val factory = GoalsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        setupRecyclerView()
        setupButtons()

        // Observe LiveData for goals from Firebase
        viewModel.allGoals.observe(this) { goals ->
            adapter.updateGoals(goals)
            updateSummary(goals)
        }

        // Observe for any errors during data fetching
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the goals list every time the activity is shown.
        // The ViewModel is already null-safe, so this call is fine.
        viewModel.loadAllGoals(currentUserId)
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAddGoal.setOnClickListener {
            // Navigate to EditGoal to create a new goal (no ID is passed)
            val intent = Intent(this, EditGoal::class.java)
            startActivity(intent)
        }
    }

    /**
     * Configures the RecyclerView with the new GoalAdapter.
     */
    private fun setupRecyclerView() {
        adapter = GoalAdapter(
            mutableListOf(),
            onEdit = { goal ->
                // Launch EditGoal activity, passing the String ID of the goal
                val intent = Intent(this, EditGoal::class.java)
                intent.putExtra("goalId", goal.id)
                startActivity(intent)
            },
            onDelete = { goal ->
                // The viewModel is already made null-safe. This call is now secure.
                // Assuming deleteGoal now takes a goalId string.
                viewModel.deleteGoal(currentUserId, goal.id)
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(this)
        binding.rvGoals.adapter = adapter
    }

    /**
     * Updates the summary section. Now uses List<Goal>.
     */
    private fun updateSummary(goals: List<Goal>) { // Removed default value
        val totalGoals = goals.size
        val completedGoals = goals.count { it.completed }

        binding.tvTotalGoals.text = totalGoals.toString()
        binding.tvCompletedGoals.text = completedGoals.toString()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}