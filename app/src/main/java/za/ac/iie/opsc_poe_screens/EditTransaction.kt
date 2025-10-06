package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for editing an existing transaction.
 * Supports editing amount, description, category, date, receipt, and recurring status.
 */
class EditTransaction : AppCompatActivity() {

    // UI Elements
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerAccount: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvDate: TextView
    private lateinit var cbRecurring: CheckBox
    private lateinit var btnPickDate: Button
    private lateinit var btnAttachReceipt: Button
    private lateinit var btnViewReceipt: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // Camera capture
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var currentPhotoFile: File? = null
    private lateinit var photoUri: Uri

    // Transaction data
    private var selectedDate: Date = Date()
    private var transactionId: Int = -1
    private var transactionEntity: TransactionEntity? = null

    // Database references
    private lateinit var database: AppDatabase
    private var accountsList: List<AccountEntity> = emptyList()
    private var categoriesList: List<CategoryEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_edit_transaction)

        // Hide UI
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions

        // Initialize UI references
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerAccount = findViewById(R.id.spinnerAccount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvDate = findViewById(R.id.tvDate)
        cbRecurring = findViewById(R.id.cbRecurring)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnAttachReceipt = findViewById(R.id.btnAttachReceipt)
        btnViewReceipt = findViewById(R.id.btnViewReceipt)
        btnSave = findViewById(R.id.btnSaveTransaction)
        btnCancel = findViewById(R.id.btnCancel)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Get transaction ID from Intent
        transactionId = intent.getIntExtra("transactionId", -1)
        if (transactionId == -1) finish() // Invalid transaction ID

        // Set up camera launcher and load transaction data
        setupTakePictureLauncher()
        loadData()
        setupButtons()
        setupDatePicker()
    }

    /**
     * Load accounts, categories, and transaction data asynchronously.
     */
    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            accountsList = database.accountDao().getAllAccounts(UserSession.currentUserId)
            categoriesList = database.categoryDao().getAllCategories()
            transactionEntity = database.transactionDao().getTransactionById(transactionId)

            transactionEntity?.let { txn -> selectedDate = txn.date }

            withContext(Dispatchers.Main) {
                populateSpinners()
                bindTransaction()
            }
        }
    }

    /**
     * Populate account and category spinners with database values.
     */
    private fun populateSpinners() {
        val accountNames = accountsList.map { it.AccountName }
        spinnerAccount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            accountNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val categoryNames = categoriesList.map { it.name }
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    /**
     * Bind transaction data to UI fields for editing.
     */
    private fun bindTransaction() {
        transactionEntity?.let { txn ->
            etAmount.setText(txn.amount.toString())
            etDescription.setText(txn.description)
            spinnerAccount.setSelection(accountsList.indexOfFirst { it.id == txn.accountId })
            spinnerAccount.isEnabled = false
            spinnerAccount.isClickable = false
            spinnerCategory.setSelection(categoriesList.indexOfFirst { it.id == txn.categoryId })
            cbRecurring.isChecked = txn.recurring
            tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate)

            txn.receiptPath?.let {
                val file = File(it)
                if (file.exists()) currentPhotoFile = file
            }
        }
    }

    /**
     * Configure the date picker dialog for changing transaction date.
     */
    private fun setupDatePicker() {
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = selectedDate }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    selectedDate = cal.time
                    tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Configure buttons for saving, cancelling, attaching, and viewing receipts.
     */
    private fun setupButtons() {
        btnSave.setOnClickListener { saveTransaction() }
        btnCancel.setOnClickListener { finish() }

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
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                takePictureLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show()
                Log.e("EditTransaction", "Error launching camera", e)
            }
        }

        btnViewReceipt.setOnClickListener {
            currentPhotoFile?.let { file ->
                if (file.exists()) showReceiptDialog(file)
                else Toast.makeText(this, "Receipt file not found", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "No receipt attached", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Setup camera capture launcher to handle photo results.
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

    /**
     * Save the edited transaction to the database.
     */
    private fun saveTransaction() {
        val amount = etAmount.text.toString().toFloatOrNull()
        if (amount == null) {
            Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val description = etDescription.text.toString()
        val categoryId = categoriesList[spinnerCategory.selectedItemPosition].id
        val recurring = cbRecurring.isChecked
        val receiptPath = currentPhotoFile?.absolutePath

        val updatedTransaction = transactionEntity?.copy(
            amount = amount,
            description = description,
            categoryId = categoryId,
            recurring = recurring,
            date = selectedDate,
            receiptPath = receiptPath
        )

        updatedTransaction?.let { txn ->
            CoroutineScope(Dispatchers.IO).launch {
                database.transactionDao().updateTransaction(txn)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTransaction, "Transaction updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /**
     * Display the attached receipt image in a dialog.
     */
    private fun showReceiptDialog(file: File) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivReceipt)
        val closeButton = dialogView.findViewById<Button>(R.id.btnCloseReceipt)

        imageView.setImageURI(Uri.fromFile(file))

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
