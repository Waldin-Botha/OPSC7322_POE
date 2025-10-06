package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUp : AppCompatActivity() {
    private lateinit var userDao: UserDao
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var goalDao: GoalDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        supportActionBar?.hide()
        hideSystemUI()

        val db = AppDatabase.getDatabase(this)
        userDao = db.userDao()
        accountDao = db.accountDao()
        transactionDao = db.transactionDao()
        categoryDao = db.categoryDao()
        goalDao = db.goalDao()

        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        btnCreate.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Check if user already exists
                if (userDao.getUserByUsername(username) != null) {
                    Toast.makeText(this@SignUp, "Username already taken", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Insert new user
                val createdUserId : Long = userDao.insertUser(UserEntity(username = username, passwordHash = password))

                // Create default data for the new user
                CreateDummyDataForAccount(createdUserId.toInt())

                // --- START OF STREAK LOGIC ---
                // This is the user's first login, so set their streak.
                StreakManager.updateUserStreak(this@SignUp)
                // --- END OF STREAK LOGIC ---

                // Redirect to SignIn
                Toast.makeText(this@SignUp, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignUp, SignIn::class.java))
                finish()
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

    private suspend fun CreateDummyDataForAccount(id : Int){
        withContext(Dispatchers.IO){
            accountDao.createDefaultAccountsForUser(id)
            categoryDao.insertDefaultCategoriesForUser(id)
            goalDao.insertDefaultGoalsForUser(id)
        }
    }
}