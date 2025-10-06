package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing income and expense categories.
 * Provides LiveData streams for observing changes and methods for CRUD operations.
 *
 * @param dao DAO for accessing category data
 */
class CategoriesViewModel(private val dao: CategoryDao) : ViewModel() {

    /** LiveData emitting all income categories from the database. */
    val incomeCategories: LiveData<List<CategoryEntity>> = dao.getLiveIncomeCategories(UserSession.currentUserId)

    /** LiveData emitting all expense categories from the database. */
    val expenseCategories: LiveData<List<CategoryEntity>> = dao.getLiveExpenseCategories(UserSession.currentUserId)

    /** Adds a new category to the database asynchronously. */
    fun addCategory(category: CategoryEntity) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            dao.insertCategory(category)
        }
    }

    /** Deletes an existing category from the database asynchronously. */
    fun deleteCategory(category: CategoryEntity) = viewModelScope.launch {
        dao.deleteCategory(category)
    }

    /** Updates an existing category in the database asynchronously. */
    fun updateCategory(category: CategoryEntity) = viewModelScope.launch {
        dao.updateCategory(category)
    }
}

/**
 * Factory class for creating instances of [CategoriesViewModel] with a specific DAO.
 *
 * @param dao DAO for accessing category data
 */
class CategoriesViewModelFactory(private val dao: CategoryDao) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given [modelClass].
     *
     * @param modelClass The class of the ViewModel to create
     * @return A new instance of [CategoriesViewModel] if requested
     * @throws IllegalArgumentException if the requested class is unknown
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
