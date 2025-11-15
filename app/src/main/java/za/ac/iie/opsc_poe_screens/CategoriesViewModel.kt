package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _allCategories = MutableLiveData<List<Category>>()

    // LiveData for expense categories, derived from the full list
    val expenseCategories: LiveData<List<Category>> = _allCategories.map { categories ->
        categories.filter { !it.isIncome }
    }

    // LiveData for income categories, derived from the full list
    val incomeCategories: LiveData<List<Category>> = _allCategories.map { categories ->
        categories.filter { it.isIncome }
    }

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadCategories(userId: String?) {
        // If the userId is null, do not proceed.
        if (userId == null) {
            _error.value = "User ID is missing. Cannot load categories."
            _allCategories.value = emptyList() // Clear old data
            return
        }
        viewModelScope.launch {
            try {
                _allCategories.value = repository.getUserCategories(userId)
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    fun addCategory(userId: String?, category: Category) {
        if (userId == null) { _error.value = "Cannot add category: User not logged in."; return }
        viewModelScope.launch {
            try {
                repository.addCategory(userId, category)
                loadCategories(userId) // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun updateCategory(userId: String?, category: Category) {
        if (userId == null) { _error.value = "Cannot add category: User not logged in."; return }
        viewModelScope.launch {
            try {
                repository.updateCategory(userId, category)
                loadCategories(userId) // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
            }
        }
    }

    fun deleteCategory(userId: String?, categoryId: String) {
        if (userId == null) { _error.value = "Cannot add category: User not logged in."; return }
        viewModelScope.launch {
            try {
                repository.deleteCategory(userId, categoryId)
                loadCategories(userId) // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
            }
        }
    }
}

/**
 * Factory for creating CategoriesViewModel with a FirebaseRepository.
 */
class CategoriesViewModelFactory(
    private val repository: FirebaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
