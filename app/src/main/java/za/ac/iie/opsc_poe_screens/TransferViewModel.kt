package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel to handle the business logic for transferring funds using Firebase.
 */
class TransferViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> get() = _accounts

    private val _transferResult = MutableLiveData<Result<String>>()
    val transferResult: LiveData<Result<String>> get() = _transferResult

    /**
     * Loads all accounts for a given user.
     * NOW NULL-SAFE.
     */
    fun loadAccounts(userId: String?) { // CHANGED: userId is now nullable
        // ADDED: Guard clause for null userId
        if (userId.isNullOrBlank()) {
            _transferResult.value = Result.failure(Exception("Cannot load accounts: User is not logged in."))
            return
        }

        viewModelScope.launch {
            try {
                _accounts.value = repository.getUserAccounts(userId)
            } catch (e: Exception) {
                _transferResult.value = Result.failure(e)
            }
        }
    }

    /**
     * Executes the fund transfer logic by calling the repository.
     * NOW NULL-SAFE.
     */
    fun transferFunds(
        userId: String?, // CHANGED: userId is now nullable
        fromAccount: Account,
        toAccount: Account,
        amount: Double
    ) {
        // ADDED: Guard clause for null userId
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
