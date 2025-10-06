package za.ac.iie.opsc_poe_screens

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.opsc_poe_screens.databinding.ActivityTransferFundsBinding

/**
 * Activity for transferring funds between two accounts.
 * Shows account spinners, allows input of transfer amount,
 * and updates balances via TransferViewModel.
 */
class TransferFunds : AppCompatActivity() {

    private lateinit var binding: ActivityTransferFundsBinding
    private lateinit var viewModel: TransferViewModel
    private var accounts: List<AccountEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Hide top and bottom UI elements
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        binding = ActivityTransferFundsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel with DAOs
        val db = AppDatabase.getDatabase(this)
        val factory = TransferViewModelFactory(db.accountDao(), db.transactionDao(), db.categoryDao())
        viewModel = ViewModelProvider(this, factory)[TransferViewModel::class.java]

        setupAccountSpinners()
        setupButtons()
    }

    /** Observe account list and populate spinners */
    private fun setupAccountSpinners() {
        viewModel.allAccounts.observe(this) { accountList ->
            accounts = accountList

            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                accounts.map { "${it.AccountName} (Balance: ${it.Balance})" }
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.spFromAccount.adapter = spinnerAdapter
            binding.spToAccount.adapter = spinnerAdapter
        }
    }

    /** Set click listeners for Cancel and Transfer buttons */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnTransfer.setOnClickListener {
            val amountText = binding.etTransferAmount.text.toString()
            val amount = amountText.toFloatOrNull()

            // Validate amount
            if (amount == null || amount <= 0f) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fromIndex = binding.spFromAccount.selectedItemPosition
            val toIndex = binding.spToAccount.selectedItemPosition

            // Check that accounts are different
            if (fromIndex == toIndex) {
                Toast.makeText(this, "Choose different accounts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fromAccount = accounts[fromIndex]
            val toAccount = accounts[toIndex]

            // Check for sufficient funds
            if (fromAccount.Balance < amount) {
                Toast.makeText(this, "Insufficient funds in ${fromAccount.AccountName}", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Perform transfer
            viewModel.transferFunds(fromAccount, toAccount, amount)

            Toast.makeText(
                this,
                "Transferred $amount from ${fromAccount.AccountName} to ${toAccount.AccountName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
