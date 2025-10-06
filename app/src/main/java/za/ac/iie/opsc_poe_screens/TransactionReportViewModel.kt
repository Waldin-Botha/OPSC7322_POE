package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import java.util.Date

class TransactionReportViewModel(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val userId: Int
) : ViewModel() {

    fun getAccounts(): LiveData<List<AccountEntity>> {
        // Use the userId from the constructor
        return accountDao.getAllLiveAccounts(userId)
    }

    fun getTransactionsForAccount(accountId: Int, start: Date, end: Date): LiveData<List<TransactionWithAccountAndCategory>> {
        // Pass the userId to the DAO function
        return transactionDao.getTransactionsForAccountInRange(accountId, userId, start, end)
    }
}