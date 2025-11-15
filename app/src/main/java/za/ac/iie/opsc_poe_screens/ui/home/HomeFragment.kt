package za.ac.iie.opsc_poe_screens.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import za.ac.iie.opsc_poe_screens.*
import za.ac.iie.opsc_poe_screens.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CardAdapter
    private lateinit var viewModel: AccountsViewModel
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        // Get the userId directly from the singleton's property
        val userId = UserSession.currentUserId

        // Check if the userId is null. If so, the user is not logged in.
        if (userId == null) {
            Toast.makeText(requireContext(), "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()
            // Redirect to SignIn and clear the back stack
            val intent = Intent(requireContext(), SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            // No need to call activity?.finish() because of the flags
            return root // Stop executing further code in this method
        }

        // If we reach here, userId is not null. Store it in the class variable.
        currentUserId = userId

        // --- Step 2: Initialize ViewModel with FirebaseRepository ---
        // No more AppDatabase!
        val repository = FirebaseRepository()
        val factory = AccountsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AccountsViewModel::class.java]

        // --- Step 3: Setup UI and Observers ---
        binding.monthTextView.text = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        setupRecyclerView()
        setupObservers() // Renamed for clarity
        setupClickListeners()

        // --- Step 4: Trigger data loading ---
        viewModel.loadDashboardData(currentUserId)

        return root
    }

    private fun setupRecyclerView() {
        adapter = CardAdapter(
            mutableListOf(),
            onAddClick = { showAddAccountDialog() },
            onCardClick = { accountWithBalance ->
                // Pass the String ID from the new Account model
                val intent = Intent(requireContext(), AccountDetail::class.java)
                intent.putExtra("accountId", accountWithBalance.account.id)
                startActivity(intent)
            }
        )
        binding.cardsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.cardsRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        // Observer for the list of accounts
        viewModel.accountsWithBalance.observe(viewLifecycleOwner) { accounts ->
            adapter.updateCards(accounts)

            // Calculate the main balance from the list of accounts
            val mainBalance = accounts.sumOf { it.balance }
            binding.tvBalance.text = String.format("R %.2f", mainBalance)
        }

        // Observer for total income
        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            binding.tvIncomeValue.text = String.format("R %.2f", income)
        }

        // Observer for total expenses
        viewModel.totalExpenses.observe(viewLifecycleOwner) { expenses ->
            binding.tvExpensesValue.text = String.format("R %.2f", abs(expenses))
        }

        // Observer for error messages
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnChart.setOnClickListener {
            startActivity(Intent(requireContext(), TransactionReport::class.java))
        }
        binding.btnTrophy.setOnClickListener {
            startActivity(Intent(requireContext(), Achievements::class.java))
        }
        binding.fabActions.setOnClickListener {
            showActionsDialog()
        }
    }

    private fun showAddAccountDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_account, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputAccountName)
        val inputBalance = dialogView.findViewById<EditText>(R.id.inputAccountBalance)
        // Get a reference to the new EditText
        val inputMaxSpend = dialogView.findViewById<EditText>(R.id.inputMaxSpend)

        // --- LOGIC TO MANAGE RADIOBUTTONS (This part is already correct) ---
        val colorButtons = listOf(
            dialogView.findViewById<RadioButton>(R.id.radioRed),
            dialogView.findViewById<RadioButton>(R.id.radioGreen),
            dialogView.findViewById<RadioButton>(R.id.radioBlue),
            dialogView.findViewById<RadioButton>(R.id.radioWhite),
            dialogView.findViewById<RadioButton>(R.id.radioYellow),
            dialogView.findViewById<RadioButton>(R.id.radioOrange),
            dialogView.findViewById<RadioButton>(R.id.radioPink),
            dialogView.findViewById<RadioButton>(R.id.radioPurple)
        )
        for (button in colorButtons) {
            button.setOnClickListener {
                for (otherButton in colorButtons) {
                    if (otherButton.id != button.id) {
                        otherButton.isChecked = false
                    }
                }
                button.isChecked = true
            }
        }
        // --- END OF RADIOBUTTON LOGIC ---

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Account")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString().trim()
                val balanceStr = inputBalance.text.toString().trim()
                // Get the value from the new max spend field
                val maxSpendStr = inputMaxSpend.text.toString().trim()

                if (name.isEmpty() || balanceStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Name and Balance cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Find which button is checked and get its corresponding color
                val checkedButtonId = colorButtons.firstOrNull { it.isChecked }?.id
                val selectedColor = when (checkedButtonId) {
                    R.id.radioRed -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    R.id.radioGreen -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    R.id.radioBlue -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                    R.id.radioWhite -> ContextCompat.getColor(requireContext(), R.color.white)
                    R.id.radioYellow -> ContextCompat.getColor(requireContext(), R.color.yellow)
                    R.id.radioOrange -> ContextCompat.getColor(requireContext(), R.color.orange)
                    R.id.radioPink -> ContextCompat.getColor(requireContext(), R.color.pink)
                    R.id.radioPurple -> ContextCompat.getColor(requireContext(), R.color.purple)
                    else -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark) // Default
                }

                // Convert string inputs to doubles, defaulting to 0.0 if empty or invalid
                val startingBalance = balanceStr.toDoubleOrNull() ?: 0.0
                val maxMonthlySpend = maxSpendStr.toDoubleOrNull() ?: 0.0

                currentUserId?.let { userId ->
                    // IMPORTANT: We need to create a new function in the ViewModel
                    // that handles creating both the account and its default goal.
                    // We'll call it 'addAccountWithDefaultGoal'.
                    viewModel.addAccountWithDefaultGoal(
                        userId = userId,
                        accountName = name,
                        startingBalance = startingBalance,
                        maxMonthlySpend = maxMonthlySpend,
                        color = selectedColor
                    )
                    Toast.makeText(requireContext(), "Account '$name' created.", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(requireContext(), "Error: User session not found.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showActionsDialog() {
        // This function does not need changes as it only starts new activities.
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_actions, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<Button>(R.id.btnCategory).setOnClickListener {
            startActivity(Intent(requireContext(), Categories::class.java))
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(requireContext(), TransferFunds::class.java))
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Check if currentUserId is initialized before trying to load data.
        // This prevents a crash if the user was logged out in the meantime.
        currentUserId?.let { userId ->
            // Tell the ViewModel to reload all the dashboard data.
            // This will fetch fresh account balances, income, and expense totals.
            viewModel.loadDashboardData(userId)
        }
    }
}