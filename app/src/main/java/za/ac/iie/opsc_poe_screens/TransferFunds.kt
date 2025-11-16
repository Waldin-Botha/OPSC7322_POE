package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.opsc_poe_screens.databinding.ActivityTransferFundsBinding

class TransferFunds : AppCompatActivity() {

    private lateinit var binding: ActivityTransferFundsBinding
    private lateinit var viewModel: TransferViewModel
    private lateinit var currentUserId: String

    // The list now holds AccountWithBalance objects
    private var accountsWithBalances: List<AccountWithBalance> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferFundsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        hideSystemUI()

        val userId = UserSession.currentUserId
        if (userId == null) {
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, SignIn::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }
        currentUserId = userId

        val repository = FirebaseRepository()
        val factory = TransferViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransferViewModel::class.java]

        setupButtons()
        observeViewModel()

        // Start loading the accounts with their live balances
        viewModel.loadAccountsWithBalances(currentUserId)
    }

    private fun observeViewModel() {
        // Observe the new LiveData
        viewModel.accountsWithBalance.observe(this) { accountList ->
            accountsWithBalances = accountList

            // Format spinner text with the new, accurate balance
            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                accountsWithBalances.map { "${it.account.accountName} (R${"%.2f".format(it.balance)})" }
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.spFromAccount.adapter = spinnerAdapter
            binding.spToAccount.adapter = spinnerAdapter
        }

        viewModel.transferResult.observe(this) { result ->
            binding.btnTransfer.isEnabled = true
            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnTransfer.setOnClickListener {
            handleTransfer()
        }
    }

    private fun handleTransfer() {
        val amount = binding.etTransferAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val fromIndex = binding.spFromAccount.selectedItemPosition
        val toIndex = binding.spToAccount.selectedItemPosition

        if (fromIndex < 0 || toIndex < 0 || accountsWithBalances.isEmpty()) {
            Toast.makeText(this, "Accounts not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromIndex == toIndex) {
            Toast.makeText(this, "Cannot transfer to the same account.", Toast.LENGTH_SHORT).show()
            return
        }

        val fromAccountWithBalance = accountsWithBalances[fromIndex]
        val toAccountWithBalance = accountsWithBalances[toIndex]

        // CRITICAL CHANGE: Check against the calculated live balance
        if (fromAccountWithBalance.balance < amount) {
            Toast.makeText(this, "Insufficient funds in ${fromAccountWithBalance.account.accountName}.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnTransfer.isEnabled = false

        // Pass the raw Account objects to the transfer function as before
        viewModel.transferFunds(currentUserId, fromAccountWithBalance.account, toAccountWithBalance.account, amount)
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}
