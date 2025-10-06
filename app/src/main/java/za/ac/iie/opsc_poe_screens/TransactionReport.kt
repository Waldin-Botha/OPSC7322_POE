package za.ac.iie.opsc_poe_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityTransactionReportBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity to display transactions for a selected account and date range.
 * Includes a RecyclerView, summary stats, and a grouped bar chart for income vs. expenses.
 */
class TransactionReport : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionReportBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: TransactionReportViewModel

    private val calendar = Calendar.getInstance()
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var accounts: List<AccountEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUI() // Use modern method for immersive mode

        binding = ActivityTransactionReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the current user ID and validate it
        val currentUserId = UserSession.currentUserId
        if (currentUserId == null || currentUserId == -1) {
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_LONG).show()
            finish() // Close the activity if the user is not valid
            return
        }

        // Initialize DAOs
        val db = AppDatabase.getDatabase(this)
        val accountDao = db.accountDao()
        val transactionDao = db.transactionDao()

        // Pass all three required arguments to the factory
        val factory = TransactionReportViewModelFactory(accountDao, transactionDao, currentUserId)
        viewModel = ViewModelProvider(this, factory)[TransactionReportViewModel::class.java]

        setupRecyclerView()
        setupAccountSpinner()
        setupDateFilters()

        // Default to last 30 days
        setLast30Days()
        refreshData() // Initial data load

        binding.btnBack.setOnClickListener {
            // Using finish() is better than creating a new MainActivity intent
            // if MainActivity is the screen that launched this one.
            finish()
        }
    }//OnCreate

    /** Setup account selection spinner and observer */
    private fun setupAccountSpinner() {
        viewModel.getAccounts().observe(this) { accs ->
            accounts = accs
            val accountNames = accs.map { it.AccountName }
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spAccountFilter.adapter = spinnerAdapter

            // Refresh data whenever a different account is selected
            binding.spAccountFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    refreshData()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    /** Setup RecyclerView with TransactionAdapter */
    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            transactions = mutableListOf(),
            onEdit = { transaction ->
                val intent = Intent(this, EditTransaction::class.java)
                intent.putExtra("transactionId", transaction.transaction.id)
                startActivity(intent)
            },
            onDelete = { transaction ->
                // Delete transaction and adjust account balance
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@TransactionReport)
                        .transactionDao()
                        .deleteTransactionAndAdjustBalance(transaction.transaction)
                }
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    /** Setup predefined date filter buttons */
    private fun setupDateFilters() {
        binding.btnLast7Days.setOnClickListener {
            setLast7Days()
            refreshData()
        }
        binding.btnLast30Days.setOnClickListener {
            setLast30Days()
            refreshData()
        }
        binding.btnCustomRange.setOnClickListener {
            pickCustomRange()
        }
    }

    /** Set filter to last 7 days */
    private fun setLast7Days() {
        val calEnd = Calendar.getInstance()
        endDate = calEnd.time

        val calStart = Calendar.getInstance()
        calStart.add(Calendar.DAY_OF_YEAR, -7)
        startDate = calStart.time
    }

    /** Set filter to last 30 days */
    private fun setLast30Days() {
        val calEnd = Calendar.getInstance()
        endDate = calEnd.time

        val calStart = Calendar.getInstance()
        calStart.add(Calendar.DAY_OF_YEAR, -30)
        startDate = calStart.time
    }

    /** Open DatePickerDialogs to select a custom start and end date */
    private fun pickCustomRange() {
        val today = Calendar.getInstance()
        // Show dialog for start date
        DatePickerDialog(this, { _, startYear, startMonth, startDay ->
            val startCal = Calendar.getInstance()
            startCal.set(startYear, startMonth, startDay)
            startDate = startCal.time

            // After picking start date, show dialog for end date
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                val endCal = Calendar.getInstance()
                endCal.set(endYear, endMonth, endDay)
                endDate = endCal.time
                refreshData() // Refresh data after both dates are selected
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()

        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** Refresh transactions based on selected account and date range */
    private fun refreshData() {
        if (startDate == null || endDate == null || accounts.isEmpty()) {
            return
        }

        val selectedIndex = binding.spAccountFilter.selectedItemPosition
        if (selectedIndex < 0 || selectedIndex >= accounts.size) {
            return
        }

        val account = accounts[selectedIndex]
        viewModel.getTransactionsForAccount(account.id, startDate!!, endDate!!)
            .observe(this) { filteredTransactions ->
                adapter.updateTransactions(filteredTransactions)
                updateSummary(filteredTransactions)
                updateBarChart(filteredTransactions)
            }
    }

    /** Update summary TextViews for income, expenses, and net */
    private fun updateSummary(transactions: List<TransactionWithAccountAndCategory>) {
        val totalIncome = transactions.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount.toDouble() }
        val totalExpense = transactions.filter { it.transaction.amount < 0 }.sumOf { it.transaction.amount.toDouble() }
        val net = totalIncome + totalExpense

        binding.tvTotalIncome.text = "R ${"%.2f".format(totalIncome)}"
        binding.tvTotalExpense.text = "R ${"%.2f".format(totalExpense)}"
        binding.tvNetGrowth.text = "R ${"%.2f".format(net)}"
    }

    /** Update grouped bar chart showing income vs. expenses per day */
    private fun updateBarChart(transactions: List<TransactionWithAccountAndCategory>) {
        val grouped = transactions.groupBy { dateFormat.format(it.transaction.date) }

        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        var index = 0f
        // Using toSortedMap ensures the dates on the chart are in chronological order
        for ((date, txns) in grouped.toSortedMap()) {
            val income = txns.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount.toDouble() }.toFloat()
            val expense = txns.filter { it.transaction.amount < 0 }.sumOf { it.transaction.amount.toDouble() }.toFloat()

            incomeEntries.add(BarEntry(index, income))
            expenseEntries.add(BarEntry(index, Math.abs(expense)))
            labels.add(date.substring(5)) // Show "MM-dd" for cleaner labels

            index += 1f
        }

        val incomeSet = BarDataSet(incomeEntries, "Income").apply {
            // Assuming you have a green color in your colors.xml
            color = ContextCompat.getColor(this@TransactionReport, R.color.green)
        }

        val expenseSet = BarDataSet(expenseEntries, "Expense").apply {
            // Assuming you have a red color in your colors.xml
            color = ContextCompat.getColor(this@TransactionReport, R.color.red)
        }

        val data = BarData(incomeSet, expenseSet)
        val groupSpace = 0.2f
        val barSpace = 0.05f
        val barWidth = 0.35f

        data.barWidth = barWidth
        binding.barChart.data = data

        // X-axis setup
        val xAxis = binding.barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

        binding.barChart.axisLeft.axisMinimum = 0f

        binding.barChart.axisRight.isEnabled = false

        binding.barChart.description.isEnabled = false
        binding.barChart.legend.isEnabled = true

        binding.barChart.xAxis.axisMinimum = 0f
        // Correctly calculate the maximum for grouping
        binding.barChart.xAxis.axisMaximum = if (labels.isNotEmpty()) labels.size.toFloat() else 0f
        binding.barChart.groupBars(0f, groupSpace, barSpace)

        binding.barChart.invalidate() // Refresh the chart
    }

    /** Hides system UI elements for a modern, immersive experience. */
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}