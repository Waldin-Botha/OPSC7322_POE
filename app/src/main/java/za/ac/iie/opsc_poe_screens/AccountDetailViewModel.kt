package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the AccountDetail activity.
 * Provides streams of transactions and account data for a specific account.
 *
 * @param accountId ID of the account to display
 * @param dao TransactionDao for accessing transaction data
 * @param accountDao AccountDao for accessing account details
 * @param userId ID of the current user
 */
class AccountDetailViewModel(
    accountId: Int,
    dao: TransactionDao,
    accountDao: AccountDao,
    userId: Int
) : ViewModel() {

    /**
     * Flow emitting a list of transactions for the specified account,
     * including their associated account and category data.
     * Uses the userId from the constructor.
     */
    val transactions: Flow<List<TransactionWithAccountAndCategory>> =
        dao.getTransactionsForAccount(accountId, userId)

    /**
     * Flow emitting the account details for the specified account ID.
     * Emits null if the account does not exist.
     */
    val account: Flow<AccountEntity?> =
        accountDao.getAccountById(accountId)
}