package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// The ViewModel now takes 'userId' as a constructor parameter.
class GoalsViewModel(
    private val dao: GoalDao,
    private val userId: Int // Add userId here
) : ViewModel() {

    // Use the 'userId' from the constructor to fetch the goals.
    val allGoals: LiveData<List<GoalEntity>> = dao.getAllGoals(userId)

    fun addGoal(goal: GoalEntity) = viewModelScope.launch {
        // Ensure the goal being added belongs to the correct user.
        if (goal.userId == userId) {
            dao.insertGoal(goal)
        }
    }

    fun updateGoal(goal: GoalEntity) = viewModelScope.launch {
        if (goal.userId == userId) {
            dao.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch {
        if (goal.userId == userId) {
            dao.deleteGoal(goal)
        }
    }
}