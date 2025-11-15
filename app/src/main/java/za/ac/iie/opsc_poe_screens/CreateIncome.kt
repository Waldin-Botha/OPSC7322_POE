package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityCreateIncomeBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for creating a new income transaction using Firebase.
 */
class CreateIncome : AppCompatActivity() {

    private lateinit var binding: ActivityCreateIncomeBinding
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // --- Firebase and Data ---
    private lateinit var repository: FirebaseRepository
    private lateinit var currentUserId: String
    private var accountList = listOf<Account>() // Use new model
    private var categoryList = listOf<Category>() // Use new model

    private var preselectedAccountId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

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

        // If we reach here, the user is logged in. Store their ID and initialize the repository.
        currentUserId = userId
        repository = FirebaseRepository()

        preselectedAccountId = intent.getStringExtra("accountId")

        hideSystemUI()

        binding = ActivityCreateIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize spinners, date picker, and buttons
        setupDatePicker()
        setupButtons()

        // Load data from Firestore
        loadSpinnerData()
    }

    /**
     * Loads accounts and income categories from Firestore and sets up the spinners.
     */
    private fun loadSpinnerData() {
        lifecycleScope.launch {
            try {
                // Fetch accounts and categories from Firestore
                accountList = repository.getUserAccounts(currentUserId)
                val allCategories = repository.getUserCategories(currentUserId)
                categoryList = allCategories.filter { it.isIncome } // Filter for income categories

                // Update UI on the main thread
                setupSpinners()

            } catch (e: Exception) {
                Toast.makeText(this@CreateIncome, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpinners() {
        // Accounts spinner
        val accountNames = accountList.map { it.accountName }
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spAccount.adapter = accountAdapter

        // Income categories spinner
        val categoryNames = categoryList.map { it.name }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCategory.adapter = categoryAdapter

        if (preselectedAccountId != null) {
            // Find the position (index) of the account in the list
            val accountIndex = accountList.indexOfFirst { it.id == preselectedAccountId }
            if (accountIndex != -1) {
                // Set the spinner to that position
                binding.spAccount.setSelection(accountIndex)
                // Disable the spinner so the user cannot change it
                binding.spAccount.isEnabled = false
            }
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setText(dateFormat.format(calendar.time))
        binding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                binding.etDate.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }
    }

    /**
     * Sets up Cancel and Create buttons.
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnCreate.setOnClickListener {
            saveIncomeTransaction()
        }
    }

    /**
     * Validates input and saves a new income transaction to Firebase.
     */
    private fun saveIncomeTransaction() {
        val amountInput = binding.etAmount.text.toString().toDoubleOrNull()
        if (amountInput == null || amountInput <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAccount = accountList.getOrNull(binding.spAccount.selectedItemPosition)
        val selectedCategory = categoryList.getOrNull(binding.spCategory.selectedItemPosition)

        if (selectedAccount == null || selectedCategory == null) {
            Toast.makeText(this, "Please select an account and category.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show a loading indicator
        //binding.progressBar.visibility = View.VISIBLE
        binding.btnCreate.isEnabled = false

        lifecycleScope.launch {
            try {
                // Create the new Transaction object
                val newTransaction = FinancialTransaction(
                    id = "", // Firestore will generate this
                    amount = amountInput, // Positive for income
                    accountId = selectedAccount.id,
                    description = binding.etDescription.text.toString().trim(),
                    categoryId = selectedCategory.id,
                    isRecurring = binding.cbRecurring.isChecked,
                    date = calendar.time,
                    receiptUrl = "" // No receipt for income
                )

                // Save the transaction to Firestore using the repository
                repository.addTransaction(currentUserId, newTransaction)

                // Success
                Toast.makeText(this@CreateIncome, "Income added successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                // Failure
                Toast.makeText(this@CreateIncome, "Failed to save income: ${e.message}", Toast.LENGTH_LONG).show()
                //binding.progressBar.visibility = View.GONE
                binding.btnCreate.isEnabled = true
            }
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
