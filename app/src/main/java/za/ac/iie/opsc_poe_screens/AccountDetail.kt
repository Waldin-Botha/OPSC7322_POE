package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.opsc_poe_screens.databinding.ActivityAccountDetailBinding
import kotlin.math.abs

class AccountDetail : AppCompatActivity() {

    private lateinit var binding: ActivityAccountDetailBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: AccountDetailViewModel

    private lateinit var accountId: String // Account ID is now a String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUI()

        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Refactored Initialization ---
        accountId = intent.getStringExtra("accountId") ?: ""
        // Get the userId directly from our UserSession singleton
        val userId = UserSession.currentUserId

        // Check if either the accountId is missing or the user is not logged in
        if (accountId.isBlank() || userId == null) {
            Toast.makeText(this, "Error: Invalid account or user session.", Toast.LENGTH_SHORT).show()
            // If the user session is the problem, redirect to sign in
            if (userId == null) {
                val intent = Intent(this, SignIn::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            finish() // Close this activity
            return
        }
        // If we reach here, userId is valid. Store it in the class variable.
        currentUserId = userId

        // Initialize ViewModel with FirebaseRepository (This part is already correct)
        val repository = FirebaseRepository()
        val factory = AccountDetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AccountDetailViewModel::class.java]

        setupRecyclerView()
        setupFooter()
        observeData()

        // Trigger the initial data load with the correct userId
        viewModel.loadAccountDetails(currentUserId, accountId)
    }

    private fun setupRecyclerView() {
        // NOTE: Your TransactionAdapter must be updated to use `TransactionDetails`
        adapter = TransactionAdapter(
            onEdit = { transactionDetails ->
                val intent = Intent(this, EditTransaction::class.java)
                intent.putExtra("transactionId", transactionDetails.transaction.id)
                startActivity(intent)
            },
            onDelete = { transactionDetails ->
                // Call the ViewModel to handle deletion
                viewModel.deleteTransaction(currentUserId, transactionDetails.transaction.id, accountId)
            },
            transactions = mutableListOf()
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun observeData() {
        // Observe the account details
        viewModel.account.observe(this) { account ->
            binding.tvAccountTitle.text = account?.accountName ?: "Account Details"
        }

        // Observe the list of transactions
        viewModel.transactionDetails.observe(this) { transactions ->
            // Update the adapter, sorting by date
            adapter.updateTransactions(transactions.sortedByDescending { it.transaction.date })
            // Update the UI with new totals
            updateBalancesAndChart(transactions)
        }

        // Observe for any errors
        viewModel.error.observe(this) { error ->
            if (error.isNotBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateBalancesAndChart(transactions: List<TransactionDetails>) {
        // Calculate income, expense, and final balance from the list of transactions
        val income = transactions.filter { it.transaction.amount >= 0 }
            .sumOf { it.transaction.amount }

        val expense = transactions.filter { it.transaction.amount < 0 }
            .sumOf { it.transaction.amount }

        val currentBalance = income + expense
        binding.tvBalance.text = "R ${"%.2f".format(currentBalance)}"

        // Update the radial chart (pass Float values)
        binding.radialBalanceView.setBalances(income.toFloat(), abs(expense.toFloat()))

        // Update the total summary text views
        binding.tvTotalIncome.text = "R ${"%.2f".format(income)}"
        binding.tvTotalExpense.text = "R ${"%.2f".format(abs(expense))}"
    }

    private fun setupFooter() {
        binding.btnBack.setOnClickListener { finish() }

        // Pass the String accountId to the next activities
        binding.btnAddIncome.setOnClickListener {
            val intent = Intent(this, CreateIncome::class.java)
            intent.putExtra("accountId", accountId)
            startActivity(intent)
        }

        binding.btnAddExpense.setOnClickListener {
            val intent = Intent(this, CreateExpense::class.java)
            intent.putExtra("accountId", accountId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data every time the screen is shown to reflect changes
        // made in other activities (like adding a new transaction).
        if (::accountId.isInitialized && ::currentUserId.isInitialized) {
            viewModel.loadAccountDetails(currentUserId, accountId)
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