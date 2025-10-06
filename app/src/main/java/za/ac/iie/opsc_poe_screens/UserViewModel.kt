package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers // 1. Add this import
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // 2. Add this import

class UserViewModel(private val userDao: UserDao) : ViewModel() {

    // Function to update the user in the database
    fun updateUser(user: UserEntity) = viewModelScope.launch {
        // Use withContext to perform the database operation on a background I/O thread
        withContext(Dispatchers.IO) { // 3. Wrap the call
            userDao.updateUser(user)
        }
    }

    // Function to delete the user from the database
    fun deleteUser(user: UserEntity) = viewModelScope.launch {
        // Also wrap this call in the same way
        withContext(Dispatchers.IO) { // 4. Wrap the call
            userDao.deleteUser(user)
        }
    }

    // Function to get a user by their ID
    suspend fun getUser(userId: Int): UserEntity? {
        // This function can also be dispatched to the I/O thread for safety
        return withContext(Dispatchers.IO) { // 5. Wrap the call
            userDao.getUserById(userId)
        }
    }
}

// Factory to create the UserViewModel
class UserViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}