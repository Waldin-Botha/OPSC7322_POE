package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.opsc_poe_screens.databinding.FragmentGoalOverviewBinding
/**
 * Fragment displaying a list of goals in a RecyclerView.
 * Shows summary of total and completed goals, and allows navigation to add or edit goals.
 */
class GoalOverviewFragment : Fragment() {

    // View binding for fragment layout
    private var _binding: FragmentGoalOverviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GoalAdapter
    private lateinit var viewModel: GoalsViewModel

    private lateinit var _btnStreaks: ImageButton
    private lateinit var _btnGoals: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            _btnStreaks = btnStreaks
            _btnGoals = btnGoals
        }


        // Initialize ViewModel with the DAO and current user ID
        val dao = AppDatabase.getDatabase(requireContext()).goalDao()
        val currentUserId = UserSession.currentUserId ?: -1 // Get the current user ID
        // The factory now correctly receives both arguments
        val factory = GoalsViewModelFactory(dao, currentUserId)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        setupRecyclerView()
        updateSummary() // Initial summary with empty list

        // Handle adding a new goal
        binding.btnAddGoal.setOnClickListener {
            val intent = Intent(requireContext(), EditGoal::class.java)
            startActivity(intent)
        }

        // Observe LiveData for changes in goals and update UI
        viewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            adapter.updateGoals(goals)
            updateSummary(goals)
        }

        _btnGoals.setOnClickListener{
            val intent = Intent(requireContext(), Achievements::class.java)
            startActivity(intent)
        }

        _btnStreaks.setOnClickListener{
            val intent = Intent(requireContext(), Achievements::class.java)
            startActivity(intent)
        }
    }

    /**
     * Configures the RecyclerView and attaches GoalAdapter.
     * Sets up callbacks for edit and delete actions.
     */
    private fun setupRecyclerView() {
        adapter = GoalAdapter(
            mutableListOf(),
            onEdit = { goal ->
                // Launch EditGoal activity with the selected goal's ID
                val intent = Intent(requireContext(), EditGoal::class.java)
                intent.putExtra("goalId", goal.id)
                startActivity(intent)
            },
            onDelete = { goal ->
                // Delete the selected goal via ViewModel
                viewModel.deleteGoal(goal)
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = adapter
    }

    /**
     * Updates the summary section showing total and completed goals.
     * @param goals List of current goals to summarize.
     */
    private fun updateSummary(goals: List<GoalEntity> = listOf()) {
        val totalGoals = goals.size
        val completedGoals = goals.count { it.Completed }

        binding.tvTotalGoals.text = totalGoals.toString()
        binding.tvCompletedGoals.text = completedGoals.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}