package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityAccountDetailBinding

/**
 * Activity displaying details of a single account.
 * - Shows account title and balance.
 * - Lists all transactions in a RecyclerView.
 * - Provides buttons to add income or expense transactions.
 * - Displays a radial balance chart for income vs. expense.
 */
class AccountDetail : AppCompatActivity() {

    private lateinit var binding: ActivityAccountDetailBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: AccountDetailViewModel

    private var accountId: Int = -1

    private var currentAccount: AccountEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar and system UI
        supportActionBar?.hide()
        hideSystemUI()

        // Initialize view binding
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve accountId from intent
        accountId = intent.getIntExtra("accountId", -1)

        // Get the current user ID and validate both IDs
        val currentUserId = UserSession.currentUserId
        if (accountId == -1 || currentUserId == null || currentUserId == -1) {
            Toast.makeText(this, "Error: Invalid account or user session.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize DAOs
        val transactionDao = AppDatabase.getDatabase(this).transactionDao()
        val accountDao = AppDatabase.getDatabase(this).accountDao()

        // Create the factory with all required parameters, including the userId
        val factory = AccountDetailViewModelFactory(accountId, transactionDao, accountDao, currentUserId)

        // Create the ViewModel using the factory
        viewModel = ViewModelProvider(this, factory)[AccountDetailViewModel::class.java]

        setupRecyclerView()
        setupFooter()
        observeData()
    }

    /**
     * Sets up the RecyclerView for displaying transactions.
     * Provides callbacks for editing and deleting transactions.
     */
    private fun setupRecyclerView() {
        val dao = AppDatabase.getDatabase(this).transactionDao()
        adapter = TransactionAdapter(
            onEdit = { transaction ->
                // Open EditTransaction activity for the selected transaction
                val intent = Intent(this, EditTransaction::class.java)
                intent.putExtra("transactionId", transaction.transaction.id)
                startActivity(intent)
            },
            onDelete = { transaction ->
                // Delete transaction and adjust account balance
                lifecycleScope.launch {
                    dao.deleteTransactionAndAdjustBalance(transaction.transaction)
                }
            },
            transactions = mutableListOf()
        )

        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    /**
     * Observes account and transaction data from the ViewModel.
     * Updates RecyclerView, radial balance, and account details.
     */
    private fun observeData() {
        // Observe transactions
        lifecycleScope.launch {
            viewModel.transactions.collect { transactions ->
                adapter.updateTransactions(transactions.sortedByDescending { it.transaction.date })
                updateRadialBalance(transactions)
            }
        }

        // Observe account info
        lifecycleScope.launch {
            viewModel.account.collect { account ->
                currentAccount = account
                if (account != null) {
                    binding.tvAccountTitle.text = account.AccountName
                }
            }
        }
    }

    /**
     * Updates the radial balance view and summary text views.
     * @param transactions List of transactions for this account
     */
    private fun updateRadialBalance(transactions: List<TransactionWithAccountAndCategory>) {
        val initialBalance = currentAccount?.Balance ?: 0f

        val income = transactions.filter { it.transaction.amount >= 0 }
            .sumOf { it.transaction.amount.toDouble() }
            .toFloat()

        val expense = transactions.filter { it.transaction.amount < 0 }
            .sumOf { it.transaction.amount.toDouble() }
            .toFloat()

        val currentBalance = initialBalance
        binding.tvBalance.text = "R ${"%.2f".format(currentBalance)}"

        binding.radialBalanceView.setBalances(income, -expense) // Use negative expense for visual representation

        binding.tvTotalIncome.text = "R ${"%.2f".format(income)}"
        binding.tvTotalExpense.text = "R ${"%.2f".format(-expense)}" // Show expense as a positive number
    }

    /**
     * Sets up footer buttons: Back, Add Income, Add Expense.
     */
    private fun setupFooter() {
        binding.btnBack.setOnClickListener { finish() }

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

    /**
     * Hides system UI elements for an immersive experience.
     */
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}