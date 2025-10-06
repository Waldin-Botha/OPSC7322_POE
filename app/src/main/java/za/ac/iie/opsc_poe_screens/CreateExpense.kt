package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityCreateExpenseBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for creating a new expense transaction.
 * Allows selecting account, expense category, date, and attaching a receipt photo.
 */
class CreateExpense : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExpenseBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var db: AppDatabase
    private var accountList = listOf<AccountEntity>()
    private var categoryList = listOf<CategoryEntity>()

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var currentPhotoFile: File? = null
    private lateinit var photoUri: Uri

    private lateinit var btnAttachReceipt: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Hide UI
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions

        binding = ActivityCreateExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnAttachReceipt = binding.btnAttachReceipt

        db = AppDatabase.getDatabase(this)

        // Initialize camera launcher, date picker, and buttons
        setupTakePictureLauncher()
        setupDatePicker()
        setupButtons()

        // Load accounts and expense categories from Room asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            accountList = db.accountDao().getAllAccounts(UserSession.currentUserId)
            categoryList = db.categoryDao().getExpenseCategories(UserSession.currentUserId) // expenses only

            // Update UI on main thread after loading
            launch(Dispatchers.Main) {
                setupSpinners()
            }
        }
    }

    /**
     * Sets up the Account and Category spinners with names from the database.
     */
    private fun setupSpinners() {
        // Accounts spinner
        val accountNames = accountList.map { it.AccountName }
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spAccount.adapter = accountAdapter

        // Expense categories spinner
        val categoryNames = categoryList.map { it.name }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCategory.adapter = categoryAdapter
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
     * Sets up buttons: Cancel, Create, and Attach Receipt.
     */
    private fun setupButtons() {
        // Cancel button closes the activity
        binding.btnCancel.setOnClickListener { finish() }

        // Create button inserts expense transaction into DB
        binding.btnCreate.setOnClickListener {
            val amountInput = binding.etAmount.text.toString().toFloatOrNull()
            if (amountInput == null) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedAccount = accountList.getOrNull(binding.spAccount.selectedItemPosition)
            val selectedCategory = categoryList.getOrNull(binding.spCategory.selectedItemPosition)

            if (selectedAccount == null || selectedCategory == null) {
                Toast.makeText(this, "Please select account and category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val photoAbsolutePath = currentPhotoFile?.absolutePath

            val expenseTransaction = TransactionEntity(
                amount = -amountInput, // negative for expense
                accountId = selectedAccount.id,
                description = binding.etDescription.text.toString(),
                categoryId = selectedCategory.id,
                recurring = binding.cbRecurring.isChecked,
                date = calendar.time,
                receiptPath = photoAbsolutePath,
                userId = UserSession.currentUserId
            )

            // Insert transaction and update account balance asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                db.transactionDao().insert(expenseTransaction)
                db.accountDao().updateBalance(selectedAccount.id, selectedAccount.Balance - amountInput)

                launch(Dispatchers.Main) {
                    Toast.makeText(this@CreateExpense, "Expense added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        // Attach receipt button launches camera
        btnAttachReceipt.setOnClickListener {
            try {
                currentPhotoFile = File.createTempFile(
                    "IMG_${System.currentTimeMillis()}",
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                )
                photoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    currentPhotoFile!!
                )
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show()
                Log.e("CreateExpense", "Error launching camera", e)
            }
        }
    }

    /**
     * Sets up the ActivityResultLauncher to handle camera capture result.
     */
    private fun setupTakePictureLauncher() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show()
                } else {
                    currentPhotoFile?.delete()
                    currentPhotoFile = null
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
