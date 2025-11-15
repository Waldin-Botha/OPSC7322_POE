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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.databinding.ActivityTransactionReportBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class TransactionReport : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionReportBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: TransactionReportViewModel
    private lateinit var currentUserId: String

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var allAccounts: List<Account> = emptyList()
    private var allTransactionDetails: List<TransactionDetails> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        hideSystemUI()

        // Get the user ID from our manual UserSession singleton
        val userId = UserSession.currentUserId

        // Check if the user is actually logged in
        if (userId == null) {
            // User is not logged in. Redirect them and stop loading this screen.
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
            return
        }

        // If we reach here, the user is logged in. Store their ID.
        currentUserId = userId

        val repository = FirebaseRepository()
        val factory = TransactionReportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransactionReportViewModel::class.java]

        setupRecyclerView()
        setupDateFilters()
        observeViewModel()

        // Set default date range and trigger initial data load
        setLast30Days()
        viewModel.loadUserAccounts(currentUserId)
        refreshData()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        // Observer for the accounts spinner
        viewModel.accounts.observe(this) { accounts ->
            this.allAccounts = accounts
            val accountNames = accounts.map { it.accountName }
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spAccountFilter.adapter = spinnerAdapter

            binding.spAccountFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    // When an account is selected, filter the already-loaded list
                    filterAndDisplayData()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        // Observer for the transaction details list from the ViewModel
        viewModel.transactionDetails.observe(this) { detailsList ->
            this.allTransactionDetails = detailsList // Store the full list
            filterAndDisplayData() // Filter and display based on the current spinner selection
        }

        // Observer for errors
        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    // New method to filter the master list and update the UI
    private fun filterAndDisplayData() {
        if (allAccounts.isEmpty()) return

        val selectedAccountIndex = binding.spAccountFilter.selectedItemPosition
        if (selectedAccountIndex < 0) return

        val selectedAccountId = allAccounts[selectedAccountIndex].id
        val displayedList = allTransactionDetails.filter { it.account.id == selectedAccountId }
        val selectedAccount = allAccounts.find { it.id == selectedAccountId } ?: return

        adapter.updateTransactions(displayedList)
        updateSummary(displayedList)
        //updateBarChart(displayedList)
        updateLineChart(displayedList, selectedAccount)

    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            transactions = mutableListOf(),
            onEdit = { details ->
                val intent = Intent(this, EditTransaction::class.java)
                intent.putExtra("transactionId", details.transaction.id)
                startActivity(intent)
            },
            onDelete = { details ->
                val repo = FirebaseRepository()
                lifecycleScope.launch {
                    try {
                        repo.deleteTransaction(currentUserId, details.transaction.id)
                        Toast.makeText(this@TransactionReport, "Transaction deleted", Toast.LENGTH_SHORT).show()
                        refreshData() // Reload all data from Firebase after deletion
                    } catch (e: Exception) {
                        Toast.makeText(this@TransactionReport, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    // This method now just triggers a full data reload from Firebase
    private fun refreshData() {
        if (startDate == null || endDate == null) return
        viewModel.loadTransactionDetails(currentUserId, startDate!!, endDate!!)
    }

    private fun updateSummary(detailsList: List<TransactionDetails>) {
        val totalIncome = detailsList.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount }
        val totalExpense = detailsList.filter { it.transaction.amount < 0 }.sumOf { it.transaction.amount }
        val net = totalIncome + totalExpense

        binding.tvTotalIncome.text = "R ${"%.2f".format(totalIncome)}"
        binding.tvTotalExpense.text = "R ${"%.2f".format(abs(totalExpense))}"
        binding.tvNetGrowth.text = "R ${"%.2f".format(net)}"
    }

    private fun updateLineChart(detailsList: List<TransactionDetails>, selectedAccount: Account) {
        val chart: LineChart = findViewById(R.id.lineChart) // Get the chart view
        chart.description.isEnabled = false // Clean up the chart's appearance
        chart.axisRight.isEnabled = false

        // 1. Group transactions by date and create cumulative entries
        val transactionsByDate = detailsList.sortedBy { it.transaction.date }
            .groupBy { dateFormat.format(it.transaction.date) }

        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        var cumulativeIncome = 0f
        var cumulativeExpense = 0f
        var index = 0f

        for ((date, txns) in transactionsByDate) {
            labels.add(date.substring(5)) // Use a shorter date label like "11-15"

            cumulativeIncome += txns.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount }.toFloat()
            cumulativeExpense += txns.filter { it.transaction.amount < 0 }.sumOf { abs(it.transaction.amount) }.toFloat()

            incomeEntries.add(Entry(index, cumulativeIncome))
            expenseEntries.add(Entry(index, cumulativeExpense))
            index++
        }

        // 2. Create the DataSets for Income (Green) and Expenses (Orange)
        val incomeSet = LineDataSet(incomeEntries, "Cumulative Income").apply {
            color = ContextCompat.getColor(this@TransactionReport, R.color.green)
            valueTextColor = ContextCompat.getColor(this@TransactionReport, R.color.white)
            setCircleColor(ContextCompat.getColor(this@TransactionReport, R.color.green))
            lineWidth = 2f
        }

        val expenseSet = LineDataSet(expenseEntries, "Cumulative Expenses").apply {
            color = ContextCompat.getColor(this@TransactionReport, R.color.orange) // Use orange as requested
            valueTextColor = ContextCompat.getColor(this@TransactionReport, R.color.white)
            setCircleColor(ContextCompat.getColor(this@TransactionReport, R.color.orange))
            lineWidth = 2f
        }

        // 3. Handle the Max Spending Limit Line
        chart.axisLeft.removeAllLimitLines() // Clear old limit lines before adding a new one
        if (selectedAccount.maxMonthlySpend > 0) {
            val spendingLimit = LimitLine(selectedAccount.maxMonthlySpend.toFloat(), "Spending Limit").apply {
                lineWidth = 2f
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(this@TransactionReport, R.color.red)
                lineColor = ContextCompat.getColor(this@TransactionReport, R.color.red)
            }
            chart.axisLeft.addLimitLine(spendingLimit)
            // Adjust the axis max to make sure the limit line is always visible
            val yMax = maxOf(cumulativeIncome, cumulativeExpense, selectedAccount.maxMonthlySpend.toFloat()) * 1.1f
            chart.axisLeft.axisMaximum = yMax
        } else {
            // If there's no limit, just set the max based on the data
            val yMax = maxOf(cumulativeIncome, cumulativeExpense) * 1.1f
            chart.axisLeft.axisMaximum = yMax
        }
        chart.axisLeft.axisMinimum = 0f // Start Y-axis at 0

        // 4. Configure the X-Axis with date labels
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(this@TransactionReport, R.color.white)
        }
        chart.axisLeft.textColor = ContextCompat.getColor(this@TransactionReport, R.color.white)


        // 5. Combine everything and refresh the chart
        val lineData = LineData(incomeSet, expenseSet)
        chart.data = lineData
        chart.invalidate() // Refresh the chart
    }

    // --- Unchanged Helper Methods ---
    private fun setupDateFilters() {
        binding.btnLast7Days.setOnClickListener { setLast7Days(); refreshData() }
        binding.btnLast30Days.setOnClickListener { setLast30Days(); refreshData() }
        binding.btnCustomRange.setOnClickListener { pickCustomRange() }
    }
    private fun setLast7Days() {
        endDate = Calendar.getInstance().time
        startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
    }
    private fun setLast30Days() {
        endDate = Calendar.getInstance().time
        startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
    }
    private fun pickCustomRange() {
        val today = Calendar.getInstance()
        DatePickerDialog(this, { _, sy, sm, sd ->
            startDate = Calendar.getInstance().apply { set(sy, sm, sd) }.time
            DatePickerDialog(this, { _, ey, em, ed ->
                endDate = Calendar.getInstance().apply { set(ey, em, ed) }.time
                refreshData()
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
    }
}