package za.ac.iie.opsc_poe_screens

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import za.ac.iie.opsc_poe_screens.databinding.ActivityCategoriesBinding
import za.ac.iie.opsc_poe_screens.ui.CategoriesListFragment
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
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions

        // Inflate the layout using view binding
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val btnIncome = binding.btnIncome
        val btnExpense = binding.btnExpense
        val fragmentContainer = binding.fContent

        // Show income categories fragment on click
        btnIncome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(fragmentContainer.id, CategoriesListFragment())
                .commit()
        }

        // Show expense categories fragment on click
        btnExpense.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(fragmentContainer.id, ExpensesCategoriesListFragment())
                .commit()
        }
    }
}
