package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class GoalsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _allGoals = MutableLiveData<List<Goal>>()
    val allGoals: LiveData<List<Goal>> = _allGoals

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * Fetches all goals for a given user from Firestore.
     */
    fun loadAllGoals(userId: String?) {
        // If the userId is null, do not proceed.
        if (userId == null) {
            _error.value = "User ID is missing. Cannot load goals."
            _allGoals.value = emptyList() // Clear any old data
            return
        }

        viewModelScope.launch {
            try {
                _allGoals.value = repository.getAllGoals(userId)
            } catch (e: Exception) {
                _error.value = "Failed to load goals: ${e.message}"
            }
        }
    }

    fun addGoal(userId: String?, goal: Goal) {
        if (userId == null) {
            _error.value = "Cannot add goal: User not logged in."
            return
        }
        viewModelScope.launch {
            try {
                repository.addGoal(userId, goal)
                // Refresh the list after adding
                loadAllGoals(userId)
            } catch (e: Exception) {
                _error.postValue("Failed to add goal: ${e.message}")
            }
        }
    }

    fun updateGoal(userId: String?, goal: Goal) {
        if (userId == null) {
            _error.value = "Cannot update goal: User not logged in."
            return
        }
        viewModelScope.launch {
            try {
                repository.updateGoal(userId, goal)
                // Refresh the list after updating
                loadAllGoals(userId)
            } catch (e: Exception) {
                _error.postValue("Failed to update goal: ${e.message}")
            }
        }
    }

    /**
     * Deletes a goal and then refreshes the live data list.
     * Now takes a goalId string.
     */
    fun deleteGoal(userId: String?, goalId: String) {
        // Safety check for both user and goal ID
        if (userId.isNullOrBlank() || goalId.isBlank()) {
            _error.value = "Cannot delete goal: Invalid user or goal ID."
            return
        }

        viewModelScope.launch {
            try {
                // This now assumes your repository also has a deleteGoal(userId, goalId) method
                repository.deleteGoal(userId, goalId)

                // Refresh the list to update the UI
                loadAllGoals(userId)
            } catch (e: Exception) {
                _error.postValue("Failed to delete goal: ${e.message}")
            }
        }
    }
}