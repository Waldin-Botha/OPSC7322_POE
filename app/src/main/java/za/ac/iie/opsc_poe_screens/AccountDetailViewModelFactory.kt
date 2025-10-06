package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory class for creating instances of [AccountDetailViewModel] with specific parameters.
 *
 * @param accountId ID of the account to display
 * @param transactionDao DAO for accessing transaction data
 * @param accountDao DAO for accessing account details
 * @param userId ID of the current user
 */
class AccountDetailViewModelFactory(
    private val accountId: Int,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val userId: Int // Add userId here
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given [modelClass].
     *
     * @param modelClass The class of the ViewModel to create
     * @return A new instance of [AccountDetailViewModel] if requested
     * @throws IllegalArgumentException if the requested class is unknown
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountDetailViewModel::class.java)) {
            // Pass the userId to the ViewModel's constructor
            return AccountDetailViewModel(accountId, transactionDao, accountDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}