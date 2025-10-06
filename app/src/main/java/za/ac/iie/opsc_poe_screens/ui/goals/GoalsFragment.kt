package za.ac.iie.opsc_poe_screens.ui.goals

import za.ac.iie.opsc_poe_screens.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.opsc_poe_screens.GoalOverviewFragment
import za.ac.iie.opsc_poe_screens.databinding.FragmentGoalsBinding

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Replace dashboard container with the GoalOverviewFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, GoalOverviewFragment())
            .commit()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}