package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * ViewModel responsible for handling account transfers and related transactions.
 * Provides live data for all accounts and methods to perform transfers.
 */
class TransferViewModel(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    // LiveData that observes all accounts in the database
    val allAccounts: LiveData<List<AccountEntity>> = accountDao.getAllLiveAccounts(UserSession.currentUserId)
    var incomeCatID = -1
    var expenseCatID = -1
    /**
     * Transfers funds from one account to another.
     * Creates corresponding income and expense transactions.
     *
     * @param fromAccount The account from which funds will be withdrawn.
     * @param toAccount The account to which funds will be added.
     * @param amount The amount to transfer.
     */
    fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Float
    ) {

        viewModelScope.launch {
            if (fromAccount.Balance >= amount) {
                // Deduct from the source account and add to the target account
                fromAccount.Balance -= amount
                toAccount.Balance += amount

                // Update the accounts in the database
                accountDao.updateAccount(fromAccount)
                accountDao.updateAccount(toAccount)

                withContext(Dispatchers.IO){
                    incomeCatID = categoryDao.getCategoryByName("Transfer", UserSession.currentUserId, true).id
                    expenseCatID = categoryDao.getCategoryByName("Transfer", UserSession.currentUserId, false).id
                }

                // Record the expense transaction for the source account
                transactionDao.insert(
                    TransactionEntity(
                        amount = -amount,
                        accountId = fromAccount.id,
                        description = "Transfer to ${toAccount.AccountName}",
                        categoryId = expenseCatID,
                        recurring = false,
                        date = Date(),
                        receiptPath = "",
                        userId = UserSession.currentUserId
                    )
                )

                // Record the income transaction for the target account
                transactionDao.insert(
                    TransactionEntity(
                        amount = amount,
                        accountId = toAccount.id,
                        description = "Transfer from ${fromAccount.AccountName}",
                        categoryId = incomeCatID,
                        recurring = false,
                        date = Date(),
                        receiptPath = "",
                        userId = UserSession.currentUserId
                    )
                )
            }
        }
    }
}

/**
 * Factory class for creating instances of TransferViewModel with required DAOs.
 */
class TransferViewModelFactory(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransferViewModel(accountDao, transactionDao, categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
