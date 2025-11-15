package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Date

class TransactionReportViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> get() = _accounts

    private val _transactionDetails = MutableLiveData<List<TransactionDetails>>()
    val transactionDetails: LiveData<List<TransactionDetails>> get() = _transactionDetails

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    /**
     * Fetches all accounts for the current user to populate the spinner.
     * NOW NULL-SAFE.
     */
    fun loadUserAccounts(userId: String?) { // CHANGED: userId is now nullable
        // ADDED: Guard clause for null userId
        if (userId.isNullOrBlank()) {
            _error.postValue("Cannot load accounts: User is not logged in.")
            return
        }
        viewModelScope.launch {
            try {
                _accounts.postValue(repository.getUserAccounts(userId))
            } catch (e: Exception) {
                _error.postValue("Failed to load accounts: ${e.message}")
            }
        }
    }

    /**
     * Fetches all necessary data and constructs the list of TransactionDetails.
     * NOW NULL-SAFE.
     */
    fun loadTransactionDetails(userId: String?, startDate: Date, endDate: Date) { // CHANGED: userId is now nullable
        // ADDED: Guard clause for null userId
        if (userId.isNullOrBlank()) {
            _error.postValue("Cannot load transactions: User is not logged in.")
            return
        }
        viewModelScope.launch {
            try {
                // In a production app, you might optimize this, but for now, it's clear and correct.
                val allTransactions = repository.getAllUserTransactions(userId)
                val allAccounts = repository.getUserAccounts(userId)
                val allCategories = repository.getUserCategories(userId)

                val accountMap = allAccounts.associateBy { it.id }
                val categoryMap = allCategories.associateBy { it.id }

                val filteredTransactions = allTransactions.filter { it.date in startDate..endDate }

                val detailsList = filteredTransactions.map { transaction ->
                    TransactionDetails(
                        transaction = transaction,
                        account = accountMap[transaction.accountId] ?: Account(),
                        category = categoryMap[transaction.categoryId]
                    )
                }
                _transactionDetails.postValue(detailsList)
            } catch (e: Exception) {
                _error.postValue("Failed to load transaction details: ${e.message}")
            }
        }
    }
}