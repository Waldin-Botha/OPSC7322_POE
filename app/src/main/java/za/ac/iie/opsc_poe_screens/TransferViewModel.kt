package za.ac.iie.opsc_poe_screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.log

/**
 * ViewModel to handle the business logic for transferring funds using Firebase.
 */
class TransferViewModel(private val repository: FirebaseRepository) : ViewModel() {

    // LiveData now holds the new AccountWithBalance class
    private val _accountsWithBalance = MutableLiveData<List<AccountWithBalance>>()
    val accountsWithBalance: LiveData<List<AccountWithBalance>> get() = _accountsWithBalance

    private val _transferResult = MutableLiveData<Result<String>>()
    val transferResult: LiveData<Result<String>> get() = _transferResult

    /**
     * Loads all accounts and calculates their live balances from transactions.
     */
    fun loadAccountsWithBalances(userId: String?) {
        if (userId.isNullOrBlank()) {
            _transferResult.value = Result.failure(Exception("Cannot load accounts: User is not logged in."))
            return
        }

        viewModelScope.launch {
            try {
                // Fetch both accounts and all transactions for the user
                val accounts = repository.getUserAccounts(userId)
                val allTransactions = repository.getAllUserTransactions(userId)

                // Group transactions by their accountId for efficient calculation
                val transactionsByAccountId = allTransactions.groupBy { it.accountId }

                // Create a list of AccountWithBalance objects
                val accountsWithCalculatedBalance = accounts.map { account ->
                    // Sum the amounts for this account's transactions, or default to 0.0
                    val liveBalance = transactionsByAccountId[account.id]?.sumOf { it.amount } ?: 0.0
                    AccountWithBalance(account = account, balance = liveBalance, income = 0.0, expenses = 0.0)
                }

                _accountsWithBalance.value = accountsWithCalculatedBalance

            } catch (e: Exception) {
                _transferResult.value = Result.failure(e)
            }
        }
    }

    /**
     * Executes the fund transfer logic by calling the repository.
     */
    fun transferFunds(
        userId: String?,
        fromAccount: Account,
        toAccount: Account,
        amount: Double
    ) {
        if (userId.isNullOrBlank()) {
            _transferResult.value = Result.failure(Exception("Cannot transfer funds: User is not logged in."))
            return
        }

        viewModelScope.launch {
            try {
                repository.transferFunds(userId, fromAccount, toAccount, amount)
                _transferResult.value = Result.success("Transfer successful!")
            } catch (e: Exception) {
                _transferResult.value = Result.failure(Exception("Transfer failed: ${e.message}"))
                Log.e("TransferViewModel", "Error transferring funds", e)
            }
        }
    }
}

/**
 * Factory to create an instance of the Firebase-aware TransferViewModel.
 */
class TransferViewModelFactory(private val repository: FirebaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransferViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
