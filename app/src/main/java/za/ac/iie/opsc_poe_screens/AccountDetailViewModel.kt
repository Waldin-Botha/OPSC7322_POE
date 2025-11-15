package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AccountDetailViewModel(private val repository: FirebaseRepository) : ViewModel() {

    // LiveData for the specific account being viewed
    private val _account = MutableLiveData<Account?>()
    val account: LiveData<Account?> = _account

    // LiveData for the list of transactions with their category details
    private val _transactionDetails = MutableLiveData<List<TransactionDetails>>()
    val transactionDetails: LiveData<List<TransactionDetails>> = _transactionDetails

    // LiveData for error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * Fetches all data needed for the Account Detail screen.
     */
    fun loadAccountDetails(userId: String?, accountId: String) {
        if (userId == null) {
            _error.postValue("User ID is missing. Cannot load account details.")
            return
        }
        viewModelScope.launch {
            try {
                // Fetch the account, its transactions, and all categories in parallel
                val accountData = repository.getAccountById(userId, accountId)
                val transactions = repository.getTransactionsForAccount(userId, accountId)
                val categories = repository.getUserCategories(userId)

                _account.postValue(accountData)

                // Only proceed if we successfully fetched the account data
                if (accountData != null) {
                    // Pass the fetched accountData into the combination function
                    val details = combineData(accountData, transactions, categories)
                    _transactionDetails.postValue(details)
                }

            } catch (e: Exception) {
                _error.postValue("Failed to load account details: ${e.message}")
            }
        }
    }

    /**
     * Deletes a transaction and then reloads the data to reflect the change.
     */
    fun deleteTransaction(userId: String?, transactionId: String, accountId: String) {
        if (userId == null) {
            _error.postValue("Cannot delete transaction: User not logged in.")
            return
        }
        viewModelScope.launch {
            try {
                // First, we need to get the current account balance to adjust it
                val account = repository.getAccountById(userId, accountId)
                val transactionToDelete = repository.getTransactionById(userId, transactionId)

                if (account != null && transactionToDelete != null) {
                    // Atomically delete the transaction and update the account balance
                    repository.deleteTransactionAndUpdateBalance(userId, transactionToDelete, account)

                    // Refresh the data to show the updated list and balance
                    loadAccountDetails(userId, accountId)
                } else {
                    throw Exception("Could not find account or transaction to delete.")
                }
            } catch (e: Exception) {
                _error.postValue("Failed to delete transaction: ${e.message}")
            }
        }
    }

    private fun combineData(
        account: Account,
        transactions: List<FinancialTransaction>,
        categories: List<Category>
    ): List<TransactionDetails> {
        val categoryMap = categories.associateBy { it.id }
        return transactions.map { transaction ->
            TransactionDetails(
                transaction = transaction,
                account = account, // Now we can provide the account
                category = categoryMap[transaction.categoryId]
            )
        }
    }
}