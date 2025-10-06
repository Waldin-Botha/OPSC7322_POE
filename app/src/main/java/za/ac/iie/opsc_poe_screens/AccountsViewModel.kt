package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val userId: Int // Add userId dependency
) : ViewModel() {

    // LiveData for the list of accounts with their calculated balances
    val accountsWithTransactions: LiveData<List<AccountWithTransactions>> =
        accountDao.getAllLiveAccounts(userId).switchMap { accounts ->
            liveData {
                val resultList = accounts.map { account ->
                    val income = transactionDao.getIncomeForAccount(account.id) ?: 0f
                    val expenses = transactionDao.getExpensesForAccount(account.id) ?: 0f
                    AccountWithTransactions(account, income, expenses)
                }
                emit(resultList)
            }
        }

    // LiveData for total income across all accounts for the user
    val totalIncome: LiveData<Float?> = transactionDao.getTotalIncome(userId)

    // LiveData for total expenses across all accounts for the user
    val totalExpenses: LiveData<Float?> = transactionDao.getTotalExpenses(userId)

    // Function to add a new account
    fun addAccount(account: AccountEntity) = viewModelScope.launch {
        accountDao.insert(account)
    }
}


class AccountsViewModelFactory(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val userId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountsViewModel(accountDao, transactionDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}