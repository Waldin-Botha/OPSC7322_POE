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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityEditTransactionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class EditTransaction : AppCompatActivity() {

    private lateinit var binding: ActivityEditTransactionBinding
    private val calendar: Calendar = Calendar.getInstance()

    // --- Firebase and Data ---
    private lateinit var repository: FirebaseRepository
    private lateinit var currentUserId: String
    private var transactionId: String? = null
    private var editingTransaction: FinancialTransaction? = null
    private var accountsList: List<Account> = emptyList()
    private var categoriesList: List<Category> = emptyList()

    // --- Camera and Permissions ---
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var newPhotoUri: Uri? = null // For a newly captured photo
    private var currentPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        hideSystemUI()

        // Get the user ID from our manual UserSession singleton
        val userId = UserSession.currentUserId
        transactionId = intent.getStringExtra("transactionId")

        // Check if the user is logged in AND a transactionId was passed
        if (userId == null || transactionId.isNullOrBlank()) {
            Toast.makeText(this, "Error: Invalid user session or transaction.", Toast.LENGTH_LONG).show()

            // If the problem is the user session, redirect to sign in
            if (userId == null) {
                val intent = Intent(this, SignIn::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            finish() // Close this activity
            return   // Stop executing any further code
        }

        // If we reach here, the user is logged in. Store their ID and initialize the repository.
        currentUserId = userId
        repository = FirebaseRepository()

        setupCameraLauncher()
        setupButtons()
        setupDatePicker()

        // Load all necessary data from Firestore
        loadInitialData()
    }

    private fun loadInitialData() {
        //binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Fetch all data in parallel
                val transaction = repository.getTransactionById(currentUserId, transactionId!!)
                val accounts = repository.getUserAccounts(currentUserId)
                val categories = repository.getUserCategories(currentUserId)

                if (transaction == null) {
                    Toast.makeText(this@EditTransaction, "Error: Transaction not found.", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                // Assign fetched data
                editingTransaction = transaction
                accountsList = accounts
                categoriesList = categories

                // Populate UI on the main thread
                populateSpinners()
                bindTransactionData()
                //binding.progressBar.visibility = View.GONE

            } catch (e: Exception) {
                //binding.progressBar.visibility = View.GONE
                Toast.makeText(this@EditTransaction, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateSpinners() {
        // Accounts spinner
        val accountNames = accountsList.map { it.accountName }
        binding.spinnerAccount.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Categories spinner (both income and expense)
        val categoryNames = categoriesList.map { it.name }
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun bindTransactionData() {
        editingTransaction?.let { txn ->
            calendar.time = txn.date
            binding.etAmount.setText(abs(txn.amount).toString()) // Show positive value for editing
            binding.etDescription.setText(txn.description)
            binding.cbRecurring.isChecked = txn.isRecurring
            binding.tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(txn.date)

            // Set spinner selections
            val accountIndex = accountsList.indexOfFirst { it.id == txn.accountId }
            if (accountIndex != -1) binding.spinnerAccount.setSelection(accountIndex)

            val categoryIndex = categoriesList.indexOfFirst { it.id == txn.categoryId }
            if (categoryIndex != -1) binding.spinnerCategory.setSelection(categoryIndex)

            // Disable account spinner as it shouldn't be changed
            binding.spinnerAccount.isEnabled = false

            // Show "View Receipt" button if a URL exists
            // IF SOMETING BREAKS REMOVE THE RETURN TODO remove return if stuff break
            binding.btnViewReceipt.visibility = if (txn.receiptUrl.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun setupDatePicker() {
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnSaveTransaction.setOnClickListener { saveTransaction() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnAttachReceipt.setOnClickListener { launchCamera() }

        binding.btnViewReceipt.setOnClickListener {
            editingTransaction?.receiptUrl?.let { url ->
                if (url.isNotBlank()) {
                    showReceiptDialog(url)
                }
            }
        }
    }

    private fun saveTransaction() {
        val originalTxn = editingTransaction ?: return
        val amountInput = binding.etAmount.text.toString().toDoubleOrNull()
        if (amountInput == null || amountInput <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val finalAmount = if (originalTxn.amount < 0) -amountInput else amountInput
        val selectedCategory = categoriesList.getOrNull(binding.spinnerCategory.selectedItemPosition)

        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveTransaction.isEnabled = false

        lifecycleScope.launch {
            try {
                var finalReceiptPath = originalTxn.receiptUrl

                // A new photo was taken to replace the old one.
                if (newPhotoUri != null && currentPhotoFile != null) {
                    // 1. Create a new permanent file for the new receipt.
                    val permanentFile = createPermanentImageFile()

                    // 2. Copy the newly captured photo's content to the permanent file.
                    contentResolver.openInputStream(newPhotoUri!!)?.use { input ->
                        permanentFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 3. Set the final path to the new file's path.
                    finalReceiptPath = permanentFile.absolutePath

                    // 4. IMPORTANT: Delete the OLD receipt file if it exists.
                    originalTxn.receiptUrl?.let { oldPath ->
                        if (oldPath.isNotBlank()) {
                            File(oldPath).delete()
                        }
                    }

                    // 5. Clean up the temporary file created by the camera.
                    currentPhotoFile?.delete()
                }

                // Create the updated transaction object
                val updatedTransaction = originalTxn.copy(
                    amount = finalAmount,
                    description = binding.etDescription.text.toString().trim(),
                    categoryId = selectedCategory.id,
                    isRecurring = binding.cbRecurring.isChecked,
                    date = calendar.time,
                    receiptUrl = finalReceiptPath // This is now the local path
                )

                repository.updateTransaction(currentUserId, updatedTransaction)
                Toast.makeText(this@EditTransaction, "Transaction updated!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                binding.btnSaveTransaction.isEnabled = true
                Toast.makeText(this@EditTransaction, "Failed to update transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("EditTransaction", "Error saving transaction", e)
            }
        }
    }

    // --- Camera and Dialog Logic ---

    private fun launchCamera() {
        try {
            currentPhotoFile = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            newPhotoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", currentPhotoFile!!)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, newPhotoUri)
            takePictureLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show()
            Log.e("EditTransaction", "Error launching camera", e)
        }
    }

    private fun setupCameraLauncher() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "New photo attached", Toast.LENGTH_SHORT).show()
                binding.btnAttachReceipt.text = "REPLACE PHOTO"
            } else {
                currentPhotoFile?.delete()
                newPhotoUri = null
                Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReceiptDialog(imagePath: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivReceipt)
        val closeButton = dialogView.findViewById<Button>(R.id.btnCloseReceipt)

        val imageFile = File(imagePath)

        if (imageFile.exists()) {
            // Decode the file path directly into a Bitmap and set it on the ImageView.
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            imageView.setImageBitmap(bitmap)
        } else {
            // If the file is somehow missing, show an error.
            Toast.makeText(this, "Error: Receipt file not found on device.", Toast.LENGTH_SHORT).show()
            imageView.visibility = View.GONE
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }


    private fun createPermanentImageFile(): File {
        // Create a directory named "receipts" in your app's private files folder
        val storageDir = File(filesDir, "receipts")
        if (!storageDir.exists()) {
            storageDir.mkdirs() // Create the directory if it doesn't exist
        }
        // Create the image file inside that directory
        return File.createTempFile("RECEIPT_${System.currentTimeMillis()}", ".jpg", storageDir)
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}
