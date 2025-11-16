package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// This is a new data class to hold the combined information for the UI.
// It replaces the old 'AccountWithTransactions'.

class AccountsViewModel(
    private val repository: FirebaseRepository // The only dependency is our repository
) : ViewModel() {

    // --- LiveData for the UI ---

    // Private MutableLiveData that we will update from within the ViewModel
    private val _accountsWithBalance = MutableLiveData<List<AccountWithBalance>>()
    // Public LiveData that the UI will observe. It cannot be changed from the outside.
    val accountsWithBalance: LiveData<List<AccountWithBalance>> = _accountsWithBalance

    // LiveData for the total income across all accounts
    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    // LiveData for the total expenses across all accounts
    private val _totalExpenses = MutableLiveData<Double>()
    val totalExpenses: LiveData<Double> = _totalExpenses

    // LiveData for handling errors
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // --- Data Fetching and Processing ---

    /**
     * Fetches accounts and all their related transactions, then calculates the balances.
     * This is the main function to call from the Activity/Fragment to load data.
     */
    fun loadDashboardData(userId: String?) {
        // If the userId is null, do not proceed.
        if (userId == null) {
            _error.postValue("User ID is missing. Cannot load data.")
            // Optionally clear the livedata to hide old information
            _accountsWithBalance.postValue(emptyList())
            _totalIncome.postValue(0.0)
            _totalExpenses.postValue(0.0)
            return
        }
        // ----------------------------

        viewModelScope.launch {
            try {
                // Now we know for sure that userId is not null here.
                val userAccounts = repository.getUserAccounts(userId)
                val allTransactions = repository.getAllUserTransactions(userId)

                calculateBalances(userAccounts, allTransactions)
                calculateTotals(allTransactions)

            } catch (e: Exception) {
                _error.postValue("Failed to load dashboard data: ${e.message}")
            }
        }
    }

    private fun calculateBalances(accounts: List<Account>, transactions: List<FinancialTransaction>) {
        // Group transactions by their accountId for efficient lookup
        val transactionsByAccount = transactions.groupBy { it.accountId }

        val resultList = accounts.map { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()

            // Calculate income and expenses FOR THIS SPECIFIC ACCOUNT
            val income = accountTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val expenses = accountTransactions.filter { it.amount < 0 }.sumOf { it.amount } // This will be a negative number

            // The final balance is the sum of all transactions for this account
            val newBalance = income + expenses

            // Create the AccountWithBalance object with the new fields
            AccountWithBalance(
                account = account,
                balance = newBalance,
                income = income,
                expenses = expenses
            )
        }
        _accountsWithBalance.postValue(resultList)
    }

    private fun calculateTotals(transactions: List<FinancialTransaction>) {
        // Calculate total income (positive transaction amounts)
        _totalIncome.postValue(transactions.filter { it.amount > 0 }.sumOf { it.amount })
        // Calculate total expenses (negative transaction amounts)
        _totalExpenses.postValue(transactions.filter { it.amount < 0 }.sumOf { it.amount } * -1) // Multiply by -1 to show a positive number
    }

    /**
     * Adds a new account to Firestore.
     */
    fun addAccount(userId: String?, accountName: String, startingBalance: Double, color: Int) {
        if (userId == null) {
            _error.postValue("Cannot add account: User is not logged in.")
            return
        }
        if (accountName.isBlank()) {
            _error.postValue("Account name cannot be empty.")
            return
        }
        viewModelScope.launch {
            try {
                val newAccount = Account(
                    accountName = accountName,
                    //balance = startingBalance, // This could be the initial balance field
                    colour = color
                )
                repository.addAccount(userId, newAccount)
                // After adding, refresh all the data to show the new account
                loadDashboardData(userId)
            } catch (e: Exception) {
                _error.postValue("Failed to create account: ${e.message}")
            }
        }
    }

    fun addAccountWithDefaultGoal(userId: String, accountName: String, startingBalance: Double, maxMonthlySpend: Double, color: Int) {
        viewModelScope.launch {
            try {
                // The repository will handle the logic of creating both items
                repository.createAccountAndDefaultGoal(userId, accountName, startingBalance, maxMonthlySpend, color)
                // Optionally, reload data after adding
                loadDashboardData(userId)
            } catch (e: Exception) {
                _error.value = "Failed to create account: ${e.message}"
            }
        }
    }
}

/**
 * The new ViewModelFactory.
 * Its only job is to provide the FirebaseRepository to the ViewModel.
 */
class AccountsViewModelFactory(
    private val repository: FirebaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}