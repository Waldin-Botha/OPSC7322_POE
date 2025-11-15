package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import za.ac.iie.opsc_poe_screens.databinding.ActivityCategoriesBinding
import za.ac.iie.opsc_poe_screens.ui.IncomeCategoriesListFragment
import za.ac.iie.opsc_poe_screens.ui.ExpensesCategoriesListFragment

/**
 * Activity for managing income and expense categories.
 * Provides buttons to switch between income and expense category lists
 * and hosts the corresponding fragments.
 */
class Categories : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the default ActionBar
        supportActionBar?.hide()

        //hide UI elements
        hideSystemUI()

        // First, check if the user is actually logged in.
        val userId = UserSession.currentUserId
        if (userId == null) {
            // User is not logged in. Redirect them and stop loading this screen.
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish() // Close this activity
            return   // Stop executing any further code in onCreate
        }


        // Inflate the layout using view binding
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the default fragment to show
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fContent.id, ExpensesCategoriesListFragment())
                .commit()
        }

        binding.btnIncome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(binding.fContent.id, IncomeCategoriesListFragment())
                .commit()
        }

        binding.btnExpense.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(binding.fContent.id, ExpensesCategoriesListFragment())
                .commit()
        }
    }

    // Helper function to keep onCreate clean
    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}