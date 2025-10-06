package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating a TransactionReportViewModel.
 * It now requires AccountDao, TransactionDao, and the user's ID.
 */
class TransactionReportViewModelFactory(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val userId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionReportViewModel(accountDao, transactionDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}