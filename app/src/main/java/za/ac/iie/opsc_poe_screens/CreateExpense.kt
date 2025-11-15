package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityCreateExpenseBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateExpense : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExpenseBinding
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // --- Firebase and Data ---
    private lateinit var repository: FirebaseRepository
    private lateinit var currentUserId: String
    private var accountList = listOf<Account>() // Use new model
    private var categoryList = listOf<Category>() // Use new model

    // --- Camera and Permissions ---
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var photoUri: Uri? = null // This will hold the URI of the captured photo
    private var currentPhotoFile: File? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var btnAttachReceipt: Button

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

        setupPermissionLauncher()
        hideSystemUI()

        binding = ActivityCreateExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        btnAttachReceipt = binding.btnAttachReceipt

        setupTakePictureLauncher()
        setupDatePicker()
        setupButtons()

        // Load data from Firestore
        loadSpinnerData()
    }

    private fun loadSpinnerData() {
        lifecycleScope.launch {
            try {
                // Fetch accounts and categories from Firestore
                accountList = repository.getUserAccounts(currentUserId)
                val allCategories = repository.getUserCategories(currentUserId)
                categoryList = allCategories.filter { !it.isIncome } // Filter for expense categories

                setupSpinners()
            } catch (e: Exception) {
                Toast.makeText(this@CreateExpense, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpinners() {
        // Accounts spinner
        val accountNames = accountList.map { it.accountName }
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spAccount.adapter = accountAdapter

        // Expense categories spinner
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

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnCreate.setOnClickListener {
            // This now calls the new save function
            saveExpenseTransaction()
        }

        btnAttachReceipt.setOnClickListener {
            handleCameraPermission()
        }
    }

    private fun saveExpenseTransaction() {
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

        binding.btnCreate.isEnabled = false

        lifecycleScope.launch {
            try {
                var finalReceiptPath: String? = null

                if (photoUri != null && currentPhotoFile != null) {
                    // 1. Create a permanent file in the app's private storage
                    val permanentFile = createPermanentImageFile()

                    // 2. Copy the captured photo's content to the new permanent file
                    contentResolver.openInputStream(photoUri!!)?.use { input ->
                        permanentFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 3. Get the absolute path of the new file to save in the database
                    finalReceiptPath = permanentFile.absolutePath

                    // 4. Clean up the temporary file created by the camera
                    currentPhotoFile?.delete()
                }

                // Create the new Transaction object
                val newTransaction = FinancialTransaction(
                    id = "", // Will be generated by the repository
                    amount = -amountInput, // Negative for an expense
                    accountId = selectedAccount.id,
                    description = binding.etDescription.text.toString().trim(),
                    categoryId = selectedCategory.id,
                    isRecurring = binding.cbRecurring.isChecked,
                    date = calendar.time,
                    // ** CRITICAL: Save the local file path, not a URL **
                    receiptUrl = finalReceiptPath
                )

                // Save the transaction to the Realtime Database
                repository.addTransaction(currentUserId, newTransaction)

                // Success
                Toast.makeText(this@CreateExpense, "Expense added successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                // Failure
                Toast.makeText(this@CreateExpense, "Failed to save expense: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("CreateExpense", "Error saving transaction", e) // Log the detailed error
                binding.btnCreate.isEnabled = true
            }
        }
    }

    // --- Camera and Permission Logic (Mostly Unchanged) ---

    private fun handleCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // You can show a dialog explaining why you need the permission
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to attach a receipt.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchCamera() {
        try {
            currentPhotoFile = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", currentPhotoFile!!)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            takePictureLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing camera.", Toast.LENGTH_SHORT).show()
            Log.e("CreateExpense", "Error launching camera", e)
        }
    }

    private fun setupTakePictureLauncher() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // The photo has been saved to the 'photoUri'. We don't need to do anything else here.
                Toast.makeText(this, "Photo attached successfully.", Toast.LENGTH_SHORT).show()
                binding.btnAttachReceipt.text = "PHOTO ATTACHED" // Give visual feedback
            } else {
                // If the user cancels, delete the temp file and reset the URI
                currentPhotoFile?.delete()
                currentPhotoFile = null
                photoUri = null
                Toast.makeText(this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
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
