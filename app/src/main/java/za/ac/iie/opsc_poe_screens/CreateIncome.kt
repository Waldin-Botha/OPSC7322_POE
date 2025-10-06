package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.opsc_poe_screens.databinding.ActivityCreateIncomeBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for creating a new income transaction.
 * Allows selecting account, income category, date, and recurring option.
 */
class CreateIncome : AppCompatActivity() {

    private lateinit var binding: ActivityCreateIncomeBinding
    private lateinit var db: AppDatabase

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Hide UI
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions

        binding = ActivityCreateIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // Initialize spinners, date picker, and buttons
        setupSpinners()
        setupDatePicker()
        setupButtons()
    }

    /**
     * Loads accounts and income categories from database and sets up the spinners.
     */
    private fun setupSpinners() {
        lifecycleScope.launch(Dispatchers.IO) {
            val accounts = db.accountDao().getAllAccounts(UserSession.currentUserId)
            val categories = db.categoryDao().getIncomeCategories(UserSession.currentUserId)

            withContext(Dispatchers.Main) {
                // Accounts spinner
                binding.spAccount.adapter = ArrayAdapter(
                    this@CreateIncome,
                    android.R.layout.simple_spinner_item,
                    accounts.map { it.AccountName }
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

                // Income categories spinner
                binding.spCategory.adapter = ArrayAdapter(
                    this@CreateIncome,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
        }
    }

    /**
     * Sets up the date picker for selecting the transaction date.
     */
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
     * Cancel finishes activity.
     * Create validates input, inserts income transaction, and updates account balance.
     */
    private fun setupButtons() {
        // Cancel button closes the activity
        binding.btnCancel.setOnClickListener { finish() }

        // Create button
        binding.btnCreate.setOnClickListener {
            val amount = binding.etAmount.text.toString().toFloatOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val description = binding.etDescription.text.toString()
            val date = calendar.time
            val recurring = binding.cbRecurring.isChecked

            lifecycleScope.launch(Dispatchers.IO) {
                val accounts = db.accountDao().getAllAccounts(UserSession.currentUserId)
                val categories = db.categoryDao().getIncomeCategories(UserSession.currentUserId)

                val accountId = accounts[binding.spAccount.selectedItemPosition].id
                val categoryId = categories[binding.spCategory.selectedItemPosition].id

                val transaction = TransactionEntity(
                    amount = amount,
                    description = description,
                    accountId = accountId,
                    categoryId = categoryId,
                    recurring = recurring,
                    date = date,
                    userId = UserSession.currentUserId
                )

                // Insert transaction into DB
                db.transactionDao().insert(transaction)

                // Update account balance
                val account = accounts[binding.spAccount.selectedItemPosition]
                db.accountDao().updateBalance(account.id, account.Balance + amount)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateIncome, "Income added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
