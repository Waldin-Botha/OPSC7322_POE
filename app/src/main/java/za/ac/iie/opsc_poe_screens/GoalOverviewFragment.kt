package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.opsc_poe_screens.databinding.FragmentGoalOverviewBinding

/**
 * Fragment displaying a list of goals from Firebase.
 */
class GoalOverviewFragment : Fragment() {

    private var _binding: FragmentGoalOverviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GoalAdapter
    private lateinit var viewModel: GoalsViewModel
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the user ID from our manual UserSession singleton
        val userId = UserSession.currentUserId

        // Check if the user is actually logged in
        if (userId == null) {
            // User is not logged in. Redirect them and stop loading this fragment.
            Toast.makeText(requireContext(), "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(requireContext(), SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
            return
        }

        // If we reach here, the user is logged in. Store their ID.
        currentUserId = userId


        // Initialize ViewModel with FirebaseRepository
        val repository = FirebaseRepository()
        // Use 'this' (the fragment) as the ViewModelStoreOwner
        val factory = GoalsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GoalsViewModel::class.java]

        setupRecyclerView()
        setupButtons()

        // Observe LiveData for goals and errors
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadAllGoals(currentUserId)
    }

    private fun setupButtons() {
        // This logic is already perfect, no changes needed here.
        binding.btnAddGoal.setOnClickListener {
            val intent = Intent(requireContext(), EditGoal::class.java)
            startActivity(intent)
        }
        binding.btnGoals.setOnClickListener {
            val intent = Intent(requireContext(), Achievements::class.java)
            startActivity(intent)
        }
        binding.btnStreaks.setOnClickListener {
            val intent = Intent(requireContext(), Achievements::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // This logic is already perfect, no changes needed here.
        viewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            adapter.updateGoals(goals)
            updateSummary(goals)
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = GoalAdapter(
            mutableListOf(),
            onEdit = { goal ->
                val intent = Intent(requireContext(), EditGoal::class.java)
                intent.putExtra("goalId", goal.id)
                startActivity(intent)
            },
            onDelete = { goal ->
                // The viewModel is already made null-safe in a previous step.
                // This call is now secure.
                viewModel.deleteGoal(currentUserId, goal.id)
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = adapter
    }

    private fun updateSummary(goals: List<Goal>) { // Removed default value to avoid ambiguity
        // This logic is already perfect, no changes needed here.
        val totalGoals = goals.size
        val completedGoals = goals.count { it.completed }

        binding.tvTotalGoals.text = totalGoals.toString()
        binding.tvCompletedGoals.text = completedGoals.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}