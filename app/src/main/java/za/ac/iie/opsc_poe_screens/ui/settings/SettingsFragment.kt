package za.ac.iie.opsc_poe_screens.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import za.ac.iie.opsc_poe_screens.AccountActivity
import za.ac.iie.opsc_poe_screens.databinding.FragmentSettingsBinding

/**
 * NotificationsFragment provides quick access buttons for user account management,
 * customization settings, and FAQs. Currently, FAQs are not implemented.
 */
class SettingsFragment : Fragment() {

    // View binding for this fragment
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // UI buttons
    private lateinit var btnAccounts: ImageButton
    private lateinit var btnCustomise: ImageButton
    private lateinit var btnFAQs: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the fragment layout using ViewBinding
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize buttons from the binding
        btnAccounts = binding.btnAccounts
        btnCustomise = binding.btnCustomise
        btnFAQs = binding.btnFAQs

        // Set click listener to open UserAccounts activity
        btnAccounts.setOnClickListener {
            val intent = Intent(requireContext(), AccountActivity::class.java)
            startActivity(intent)
        }

        // Set click listener to open Customise activity
        btnCustomise.setOnClickListener {
            /*val intent = Intent(requireContext(), Customise::class.java)
            startActivity(intent)*/
            Toast.makeText(requireContext(), "Not Implemented Yet", Toast.LENGTH_SHORT).show()
        }

        // Set click listener for FAQs button (not implemented yet)
        btnFAQs.setOnClickListener {
            Toast.makeText(requireContext(), "Not Implemented Yet", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by clearing binding reference
        _binding = null
    }
}
