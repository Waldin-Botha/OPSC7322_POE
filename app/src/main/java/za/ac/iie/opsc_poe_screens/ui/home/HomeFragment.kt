package za.ac.iie.opsc_poe_screens.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.opsc_poe_screens.*
import za.ac.iie.opsc_poe_screens.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CardAdapter
    private lateinit var viewModel: AccountsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val currentUserId = UserSession.currentUserId
        if (currentUserId == null || currentUserId == -1) {
            Toast.makeText(requireContext(), "User session expired. Please sign in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(requireContext(), SignIn::class.java))
            activity?.finish()
            return root
        }

        val appDb = AppDatabase.getDatabase(requireContext())
        val factory = AccountsViewModelFactory(appDb.accountDao(), appDb.transactionDao(), currentUserId)
        viewModel = ViewModelProvider(this, factory)[AccountsViewModel::class.java]

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        binding.monthTextView.text = monthFormat.format(Calendar.getInstance().time)

        setupRecyclerView()
        setupBalanceObservers()
        setupClickListeners()

        return root
    }

    private fun setupRecyclerView() {
        adapter = CardAdapter(
            mutableListOf(),
            onAddClick = { showAddAccountDialog() },
            onCardClick = { accountWithTransactions ->
                val intent = Intent(requireContext(), AccountDetail::class.java)
                intent.putExtra("accountId", accountWithTransactions.account.id)
                startActivity(intent)
            }
        )
        binding.cardsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.cardsRecyclerView.adapter = adapter
    }

    private fun setupBalanceObservers() {
        // This single observer will drive all balance updates
        viewModel.accountsWithTransactions.observe(viewLifecycleOwner) { accountsWithTransactions ->
            // Update the RecyclerView adapter
            adapter.updateCards(accountsWithTransactions)


            // Calculate totals
            var totalInitialBalance = 0f
            var totalIncome = 0f
            var totalExpenses = 0f

            accountsWithTransactions.forEach { accWithTrans ->
                totalInitialBalance += accWithTrans.account.Balance
                totalIncome += accWithTrans.totalIncome
                totalExpenses += accWithTrans.totalExpenses
            }

            val mainBalance = totalInitialBalance

            // Update the UI text views
            binding.tvBalance.text = String.format("R %.2f", mainBalance)
            binding.tvIncomeValue.text = String.format("R %.2f", totalIncome)
            binding.tvExpensesValue.text = String.format("R %.2f", abs(totalExpenses))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAddAccountDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_account, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputAccountName)
        val inputBalance = dialogView.findViewById<EditText>(R.id.inputAccountBalance)
        val colorButtons = listOf<RadioButton>(
            dialogView.findViewById(R.id.radioRed),
            dialogView.findViewById(R.id.radioGreen),
            dialogView.findViewById(R.id.radioBlue),
            dialogView.findViewById(R.id.radioWhite),
            dialogView.findViewById(R.id.radioYellow),
            dialogView.findViewById(R.id.radioOrange),
            dialogView.findViewById(R.id.radioPink),
            dialogView.findViewById(R.id.radioPurple)
        )

        var selectedColor: Int = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        colorButtons.forEach { button ->
            button.setOnClickListener {
                colorButtons.forEach { it.isChecked = false }
                button.isChecked = true
                selectedColor = when (button.id) {
                    R.id.radioGreen -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    R.id.radioBlue -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                    R.id.radioRed -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    R.id.radioOrange -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                    R.id.radioPurple -> ContextCompat.getColor(requireContext(), android.R.color.holo_purple)
                    else -> ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Account")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                // Get user input from the dialog
                val name = inputName.text.toString().trim()
                val balanceInput = inputBalance.text.toString().trim()

                if (name.isEmpty() || balanceInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter both name and balance", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton // Stop if inputs are empty
                }

                // Launch a Coroutine for Database Operations
                lifecycleScope.launch {
                    // Get the DAO instance
                    val accountDao = AppDatabase.getDatabase(requireContext()).accountDao()
                    val existingAccount = accountDao.getPossibleAccountByName(name, UserSession.currentUserId)

                    // Switch back to the Main thread to show UI feedback or save
                    withContext(Dispatchers.Main) {
                        if (existingAccount != null) {
                            // A duplicate was found, show an error message.
                            Toast.makeText(
                                requireContext(),
                                "An account with the name '$name' already exists.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // No duplicate found, proceed to create and save the new account.
                            val newAccount = AccountEntity(
                                AccountName = name,
                                Balance = balanceInput.toFloat(),
                                Colour = selectedColor,
                                userId = UserSession.currentUserId
                            )

                            // Call the ViewModel to handle the insertion.
                            viewModel.addAccount(newAccount)

                            Toast.makeText(
                                requireContext(),
                                "Account '$name' created successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showActionsDialog() {
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
}