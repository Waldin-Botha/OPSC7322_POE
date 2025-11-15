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

    // Use the Firebase 'Account' model
    private var accounts: List<Account> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferFundsBinding.inflate(layoutInflater)
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

        val repository = FirebaseRepository()
        val factory = TransferViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransferViewModel::class.java]

        setupButtons()
        observeViewModel()

        // Start loading the accounts
        viewModel.loadAccounts(currentUserId)
    }

    private fun observeViewModel() {
        // Observer for account list to populate spinners
        viewModel.accounts.observe(this) { accountList ->
            accounts = accountList

            // Format account names with their balances for display
            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                accounts.map { "${it.accountName} (R${"%.2f".format(it.balance)})" }
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.spFromAccount.adapter = spinnerAdapter
            binding.spToAccount.adapter = spinnerAdapter
        }

        // Observer for the result of the transfer operation
        viewModel.transferResult.observe(this) { result ->
            binding.btnTransfer.isEnabled = true

            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish() // Close the activity on success
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
        val amountText = binding.etTransferAmount.text.toString()
        val amount = amountText.toDoubleOrNull()

        // Validate amount
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val fromIndex = binding.spFromAccount.selectedItemPosition
        val toIndex = binding.spToAccount.selectedItemPosition

        // Check for valid spinner selections
        if (fromIndex < 0 || toIndex < 0 || accounts.isEmpty()) {
            Toast.makeText(this, "Accounts not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromIndex == toIndex) {
            Toast.makeText(this, "Cannot transfer to the same account.", Toast.LENGTH_SHORT).show()
            return
        }

        val fromAccount = accounts[fromIndex]
        val toAccount = accounts[toIndex]

        // Check for sufficient funds
        if (fromAccount.balance < amount) {
            Toast.makeText(this, "Insufficient funds in ${fromAccount.accountName}.", Toast.LENGTH_LONG).show()
            return
        }

        // Disable button
        binding.btnTransfer.isEnabled = false

        // Execute transfer via the ViewModel
        viewModel.transferFunds(currentUserId, fromAccount, toAccount, amount)
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}
